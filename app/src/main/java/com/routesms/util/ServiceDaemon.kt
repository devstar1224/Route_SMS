package com.routesms.util

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder

class ServiceDaemon: Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val filter = IntentFilter()
        filter.addAction("BroadcastReceive")
        registerReceiver(SMSReceiver(), filter)
        return super.onStartCommand(intent, flags, startId)
    }
}