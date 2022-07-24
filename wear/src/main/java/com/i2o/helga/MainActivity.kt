package com.i2o.helga

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.i2o.helga.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.Instant

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding

    private val nodeClient by lazy { Wearable.getNodeClient(this) }
    private val messageClient by lazy { Wearable.getMessageClient(this) }
    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(this) }

    private val clientDataViewModel by viewModels<ClientDataViewModel>()

    var checkAppConnectedJob: Job = Job().apply { complete() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.openApp.setOnClickListener {
            startHandheldActivity()
        }

        clientDataViewModel.appConnected.observe(this) { value ->
            checkAppConnectedJob.cancel()
            var count = 0

            if (value) {
                binding.openApp.visibility = View.GONE
//                Toast.makeText(this, "App Connected", Toast.LENGTH_SHORT).show()
            } else {
                binding.openApp.visibility = View.VISIBLE
            }
            //checks if app is still connected
            checkAppConnectedJob = lifecycleScope.launch {
                var lastTriggerTime = Instant.now() - (countInterval - Duration.ofSeconds(1))
                while (isActive) {
                    delay(
                        Duration.between(Instant.now(), lastTriggerTime + countInterval).toMillis()
                    )
                    lastTriggerTime = Instant.now()

                    if (count > 5){
                        binding.openApp.visibility = View.VISIBLE
//                        Toast.makeText(applicationContext, "App NOT Connected", Toast.LENGTH_SHORT).show()
                    }

                    Log.d(TAG, "Count: $count")

                    count++
                }
            }
        }

    }

    private fun startHandheldActivity() {
        lifecycleScope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()

                // Send a message to all nodes in parallel
                nodes.map { node ->
                    async {
                        messageClient.sendMessage(node.id, START_ACTIVITY_PATH, byteArrayOf())
                            .await()
                    }
                }.awaitAll()

                Log.d(TAG, "Starting activity requests sent successfully")
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                Log.d(TAG, "Starting activity failed: $exception")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(clientDataViewModel)
        messageClient.addListener(clientDataViewModel)
        capabilityClient.addListener(
            clientDataViewModel,
            Uri.parse("wear://"),
            CapabilityClient.FILTER_REACHABLE
        )
    }

    override fun onPause() {
        super.onPause()
        dataClient.removeListener(clientDataViewModel)
        messageClient.removeListener(clientDataViewModel)
        capabilityClient.removeListener(clientDataViewModel)
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val START_ACTIVITY_PATH = "/start-activity"
        private val countInterval = Duration.ofSeconds(5)
    }
}