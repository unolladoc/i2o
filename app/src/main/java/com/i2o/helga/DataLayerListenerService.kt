package com.i2o.helga

import android.content.Intent
import android.media.AudioRecord
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class DataLayerListenerService: WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val dataClient by lazy { Wearable.getDataClient(this) }

    private lateinit var classifier: AudioClassifier
    private lateinit var audioTensor: TensorAudio

    override fun onCreate() {
        super.onCreate()

        classifier = AudioClassifier.createFromFile(this, MODEL_FILE)
        Log.d(TAG, "classifier: $classifier")
        audioTensor = classifier.createInputTensorAudio()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            START_ACTIVITY_PATH -> {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            VOICE_TRANSCRIPTION_MESSAGE_PATH -> {
                val byteArray = messageEvent.data
                val shorts = ShortArray(byteArray.size / 2)
                ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

                startListening(shorts)
//                Log.d("DataLayerListenerService", shorts.toString())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun startListening(audioRecord: ShortArray){
        scope.launch {
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

            if (finalOutput.isEmpty()) return@launch

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
                    Log.d(TAG, "LABEL DETECTED!!!!!!!!!!!! " + category.label)
                    returnResult(category.label.lowercase())
                }
            }
        }
    }

    private suspend fun returnResult(category: String) {
        try {
            val request = PutDataMapRequest.create(RESULT_PATH).apply {
                dataMap.putString(RESULT_KEY, category)
            }
                .asPutDataRequest()
                .setUrgent()

            val result = dataClient.putDataItem(request).await()

            Log.d(TAG, "DataItem saved: $result")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d(TAG, "Saving DataItem failed: $exception")
        }
    }

    companion object {
        private const val TAG = "DataLayerService"
        private const val START_ACTIVITY_PATH = "/start-activity"
        private const val VOICE_TRANSCRIPTION_MESSAGE_PATH = "/voice_transcription"
        private const val RESULT_PATH = "/result"
        private const val RESULT_KEY = "result"

        private const val MODEL_FILE = "yamnet.tflite"
    }
}