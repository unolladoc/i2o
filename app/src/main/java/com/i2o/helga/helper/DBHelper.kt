package com.i2o.helga.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.i2o.helga.model.Notification

class DBHelper(context: Context?) :
    SQLiteOpenHelper(context, "NotificationData.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("create Table NotificationData(message TEXT, date TEXT primary key)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("drop Table if exists NotificationData")
    }

    // insert notification data to the database
    fun insertData(notification: Notification): Boolean {
        val sqLiteDatabase = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put("message", notification.getNotificationName())
        contentValues.put("date", notification.getTimeStamp())
        val result = sqLiteDatabase.insert("NotificationData", null, contentValues)
        return result != -1L
    }

    // delete notification data from the database
    fun deleteData(date: String): Boolean {
        val sqLiteDatabase = this.writableDatabase
        val cursor =
            sqLiteDatabase.rawQuery("Select * from NotificationData where name = ?", arrayOf(date))
        return if (cursor.count > 0) {
            val result = sqLiteDatabase.delete("NotificationData", "date=?", arrayOf(date)).toLong()
            cursor.close()
            result != -1L
        } else {
            false
        }
    }

    val data: Cursor
        get() {
            val sqLiteDatabase = this.writableDatabase
            return sqLiteDatabase.rawQuery("Select * from NotificationData", null)
        }
}
