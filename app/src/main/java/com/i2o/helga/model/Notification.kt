package com.i2o.helga.model

class Notification(private val timeStamp: String, private val message: String) {

    fun getTimeStamp():String {
        return timeStamp
    }

    fun getNotificationName():String {
        return message
    }

}
