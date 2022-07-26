package com.i2o.helga

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ClientDataViewModel(
    application: Application
) :
    AndroidViewModel(application),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private val _result = MutableLiveData<String>()
    val result: LiveData<String> = _result

    private val  _appConnected = MutableLiveData<Boolean>()
    val appConnected: LiveData<Boolean> = _appConnected

    private var getMessageJob: Job = Job().apply { complete() }
    private var getAppStatusJob: Job = Job().apply { complete() }

    init {
        _appConnected.value = false
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { dataEvent ->
            when (dataEvent.type) {
                DataEvent.TYPE_CHANGED -> {
                    when (dataEvent.dataItem.uri.path) {
                        RESULT_PATH -> {
                            getMessageJob.cancel()
                            getMessageJob = viewModelScope.launch {
                                _result.value = DataMapItem.fromDataItem(dataEvent.dataItem)
                                    .dataMap
                                    .getString(RESULT_KEY)
                            }
                        }
                        COUNT_PATH -> {
                            getAppStatusJob.cancel()
                            getAppStatusJob = viewModelScope.launch {
                                _appConnected.value = true
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived -> Path: ${messageEvent.path} ; Text: $messageEvent")
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged -> Path: ${capabilityInfo.name} ; Text: $capabilityInfo")
    }

    companion object {
        private const val TAG = "ClientDataViewModel"
        private const val COUNT_PATH = "/count"
        private const val COUNT_KEY = "count"
        private const val RESULT_PATH = "/result"
        private const val RESULT_KEY = "result"
    }
}
