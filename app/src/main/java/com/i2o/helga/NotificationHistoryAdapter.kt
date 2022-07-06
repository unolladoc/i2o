package com.i2o.helga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.i2o.helga.model.Notification

class NotificationHistoryAdapter(list: ArrayList<Notification?>) :
    RecyclerView.Adapter<NotificationHistoryAdapter.MyViewHolder>() {
    private var notifList: ArrayList<Notification?>

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notifTV: TextView
        val timestampTV: TextView

        init {
            notifTV = itemView.findViewById(R.id.notif_textview)
            timestampTV = itemView.findViewById(R.id.timestamp_tv)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.notif_items, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val notification: Notification? = notifList[position]
        if (notification != null) {
            holder.notifTV.text = notification.getNotificationName()
        }
        if (notification != null) {
            holder.timestampTV.text = notification.getTimeStamp()
        }
    }

    override fun getItemCount(): Int {
        return notifList.size
    }

    init {
        notifList = list
    }
}
