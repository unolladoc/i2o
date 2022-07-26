package com.i2o.helga

import android.Manifest
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.speech.RecognizerIntent
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.company.product.OverrideUnityActivity
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.i2o.helga.helper.DBHelper
import com.i2o.helga.model.Notification
import com.unity3d.player.UnityPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : OverrideUnityActivity() {

    private lateinit var notificationManager: NotificationManagerCompat

    @Volatile var isShowing:Boolean = false
    private lateinit var vibrator: Vibrator

    private lateinit var dbHelper: DBHelper
    lateinit var frameLayout: FrameLayout
    lateinit var dialog: Dialog

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val dataClient by lazy { Wearable.getDataClient(this) }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var count = 0

        scope.launch {
            var lastTriggerTime = Instant.now() - (countInterval - Duration.ofSeconds(1))
            while (isActive) {

                delay(
                    Duration.between(Instant.now(), lastTriggerTime + countInterval).toMillis()
                )
                lastTriggerTime = Instant.now()
                sendCount(count)

                count++
            }
        }

        dbHelper = DBHelper(this)
        dialog = Dialog(this)

        if (checkSelfPermission(Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.VIBRATE), 0)
        }

        val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibrator = vibratorManager.defaultVibrator

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        }

        frameLayout = findViewById(R.id.unity_frame)
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        frameLayout.addView(mUnityPlayer.view, 0, layoutParams)

    }

    override fun hearEnable(hearEnabled: Boolean) {
        if (hearEnabled) {
            startService()
        } else {
            stopService()
        }

        val enabled = hearEnabled.toString()
        UnityPlayer.UnitySendMessage("ControlsManager", "UpdateHearIcon", enabled)
    }

    override fun showNotification() {
        Log.d(TAG, "jovan showNotification ")
        val intent = Intent(this@MainActivity, NotificationActivity::class.java)
        startActivity(intent)
    }

    override fun startSpeechToText() {
        Log.d(TAG, "jovan startSpeechToText")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Start speaking")
        startActivityForResult(intent, 100)
    }

    override fun micEnable(enabled: Boolean) {
        TODO("Not yet implemented")
    }

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isShowing) return
            if (intent.action == "GET_AUDIO_LABEL") {
                val label: String = intent.extras!!.getString("LABEL").toString()
                Log.d(TAG, "label: $label")
                runOnUiThread {
                    if (label != "") {
                        showDialog(label)
                    }
                    createNotificationChannel()
                    if (label != "") {
                        showSystemNotification(applicationContext, label)
                    }
                }
            }
        }
    }

    private fun showDialog(label: String) {
        if (!saveToDB(label)) return
        vibrate()
        isShowing = true
        val timer = Timer()
        dialog.setContentView(R.layout.notification_dialog)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val imgBtn = dialog.findViewById<Button>(R.id.close_btn)
        // close dialog on button pressed
        imgBtn.setOnClickListener {
            if (dialog.isShowing) {
                closeDialog()
                timer.cancel()
                isShowing = false
                cancelNotification(NOTIFICATION_ID)
            }
        }
        dialog.setCancelable(false)
        val notificationMsg = dialog.findViewById<TextView>(R.id.notification_msg_tv)
        notificationMsg.text = label
        dialog.show()
        // close the dialog automatically after 10 seconds
        timer.schedule(object : TimerTask() {
            override fun run() {
                if (isShowing) {
                    isShowing = false
                    timer.cancel() //this will cancel the timer of the system
                    runOnUiThread { closeDialog() }
                    cancelNotification(NOTIFICATION_ID)
                }
            }
        }, 10000)
    }

    private fun closeDialog() {
        if (dialog.isShowing) {
            dialog.dismiss()
            isShowing = false
        }
    }

    @Synchronized
    private fun saveToDB(label: String): Boolean {
        val timeStamp =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"))
        return dbHelper.insertData(Notification(timeStamp, label))
    }

    private fun vibrate() {
        vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun startService() {
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter("GET_AUDIO_LABEL"))
        val intent = Intent(this, AudioDetectionService::class.java)
        startService(intent)
    }

    private fun stopService() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        val intent = Intent(this, AudioDetectionService::class.java)
        stopService(intent)
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val name: CharSequence = getString(R.string.channel_name)
        val description = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(
            CHANNEL_ID,
            name,
            importance
        )
        channel.description = description
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun showSystemNotification(context: Context, label: String) {
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_hearing_24)
                .setContentTitle("Helga")
                .setContentText(label)
                .setAutoCancel(true) // Testing
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(label)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
        notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(
            NOTIFICATION_ID,
            builder.build()
        )
    }

    private fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId) // Cancels notification
    }
    private suspend fun sendCount(count: Int) {
        try {
            val request = PutDataMapRequest.create(COUNT_PATH).apply {
                dataMap.putInt(COUNT_KEY, count)
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
        private val TAG: String = MainActivity::class.java.name
        private const val CHANNEL_ID: String = "NC"
        // notificationId is a unique int for each notification that you must define
        private const val NOTIFICATION_ID = 1

        private val countInterval = Duration.ofSeconds(5)
        private const val COUNT_PATH = "/count"
        private const val COUNT_KEY = "count"
    }
}