package com.i2o.helga

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.wearable.*

class ClientDataViewModel :
    ViewModel(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.map { dataEvent ->
            val title = when (dataEvent.type) {
                DataEvent.TYPE_CHANGED -> "DataItem changed"
                DataEvent.TYPE_DELETED -> "DataItem deleted"
                else -> "Unknown DataItem type"
            }
            Log.d(TAG, "onDataChanged -> Title: $title ; Text: ${dataEvent.dataItem}")
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived -> Path: ${messageEvent.path} ; Text: $messageEvent")
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged -> Path: ${capabilityInfo.name} ; Text: $capabilityInfo")
    }

    companion object {
        const val TAG = "ClientDataViewModel"
    }
}