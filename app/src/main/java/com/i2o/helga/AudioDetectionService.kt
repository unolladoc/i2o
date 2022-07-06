package com.i2o.helga

import android.app.Service
import android.content.Intent
import android.media.AudioRecord
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.util.*

class AudioDetectionService: Service() {

    companion object {
        private val TAG: String = AudioDetectionService::class.java.name
        private const val MODEL_FILE = "yamnet.tflite"
    }

    private lateinit var classifier: AudioClassifier
    private lateinit var audioTensor: TensorAudio
    private lateinit var audioRecord: AudioRecord
    private lateinit var timerTask: TimerTask

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "service onCreate")
        initialize()
    }

    // Initialize classifier and audioTensor
    private fun initialize() {
        //TODO: This is causing GPUDelegation Error
//        val options: AudioClassifierOptions = AudioClassifierOptions.builder()
//            .setBaseOptions(BaseOptions.builder().useGpu().build())
//            .setMaxResults(1)
//            .build()
//        classifier = AudioClassifier.createFromFileAndOptions(this, MODEL_FILE, options)
        classifier = AudioClassifier.createFromFile(this, MODEL_FILE)
        Log.d(TAG, "classifier: $classifier")
        audioTensor = classifier.createInputTensorAudio()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "service onStartCommand")
        startListening()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // stop listening if this service is destroyed
        stopListening()
        Log.d(TAG, "service onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    private fun startListening() {

        Log.d(TAG, "start recording")
        // Start recording
        audioRecord = classifier.createAudioRecord()
        if (audioRecord.state != AudioRecord.RECORDSTATE_RECORDING) {
            try {
                audioRecord.stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
        audioRecord.startRecording()

        timerTask = object : TimerTask() {
            override fun run() {

                Log.d(TAG, "Load latest samples")
                // Load latest samples
                audioTensor.load(audioRecord)

                Log.d(TAG, "Run inference")
                // Run inference
                val results = classifier.classify(audioTensor)
                Log.d(TAG, "class res " + results.size)

                Log.d(TAG, "Filter classifications")
                // Filter classifications
                val finalOutput: ArrayList<Category> = ArrayList()
                for (classifications in results) {
                    for (category in classifications.categories) {
                        if (category.score > 0.3f) {
                            Log.d(TAG, "category label jovan " + category.label)
                            finalOutput.add(category)
                        }
                    }
                }

                if (finalOutput.isEmpty()) return

                // create a multiline string with the filtered results
                for (category in finalOutput) {
                    val label = category.label.lowercase()
                    if (label.contains("siren")
                        || label.contains("knock")
                        || label.contains("baby cry")
                        || label.contains("infant cry")
                        || label.contains("bell")
                        || label.contains("alarm")
                        || label.contains("emergency")
                        || label.contains("buzzer")
                        || label.contains("chime")
                        || label.contains("screaming")
                        || label.contains("squeal")
                        || label.contains("engine")
                        || label.contains("vehicle")
                    ) {
                        Log.d(TAG, "label detected " + category.label)
                        val intent = Intent()
                        intent.action = "GET_AUDIO_LABEL"
                        intent.putExtra("LABEL", category.label)
                        LocalBroadcastManager.getInstance(applicationContext)
                            .sendBroadcast(intent)
                    }
                }
            }
        }
        Timer().schedule(
            timerTask, 1, 500
        )
    }

    private fun stopListening() {
        Log.d(TAG, "stop listening")
        timerTask.cancel()
        classifier.close()
        try {
            audioRecord.stop()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }
}