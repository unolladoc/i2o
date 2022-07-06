package com.i2o.helga

import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.i2o.helga.helper.DBHelper
import com.i2o.helga.model.Notification

class NotificationActivity: AppCompatActivity() {

    companion object {
        val TAG: String = NotificationActivity::class.java.name
    }

    private lateinit var dbHelper: DBHelper

    private lateinit var notificationHistoryAdapter: NotificationHistoryAdapter
    private lateinit var notifHistoryRV: RecyclerView
    private lateinit var notifList: List<Notification?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        val actionBar: ActionBar = supportActionBar!!
        actionBar.title = ""
        actionBar.setDisplayHomeAsUpEnabled(true)
        dbHelper = DBHelper(this)
        notifHistoryRV = findViewById<RecyclerView>(R.id.notif_history_list)
        notifList = ArrayList<Notification?>()
        notificationHistoryAdapter = NotificationHistoryAdapter(getNotifications())
        val linearLayoutManager = LinearLayoutManager(this)
        notifHistoryRV.layoutManager = linearLayoutManager
        notifHistoryRV.adapter = notificationHistoryAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onStop() {
        super.onStop()
        dbHelper.close()
        Log.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // get notifications from the sqlite database
    private fun getNotifications(): ArrayList<Notification?> {
        val cursor: Cursor = dbHelper.data
        if (cursor.count == 0) return ArrayList()
        val notifications: ArrayList<Notification?> = ArrayList()
        while (cursor.moveToNext()) {
            val message = cursor.getString(0)
            val date = cursor.getString(1)
            notifications.add(0, Notification(message, date))
        }
        cursor.close()
        return notifications
    }
}
