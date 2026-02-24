package com.routesms.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.routesms.R
import com.routesms.RouteSmsApplication
import com.routesms.bind.MainActivity

class ServiceDaemon : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureSilentChannel()
        doStartForeground(buildNotification())
        return START_STICKY
    }

    private fun doStartForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * 상태바에 아이콘이 보이지 않도록 IMPORTANCE_MIN 채널만 사용한다.
     * 기존 CHANNEL_FOREGROUND(보이는 채널)는 삭제한다.
     */
    private fun ensureSilentChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(
                NotificationChannel(
                    RouteSmsApplication.CHANNEL_FOREGROUND_SILENT,
                    "서비스 상태",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "문자전달 서비스 실행 상태"
                    setShowBadge(false)
                }
            )
            // 이전 버전에서 생성된 보이는 채널 정리
            manager.deleteNotificationChannel(RouteSmsApplication.CHANNEL_FOREGROUND)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, RouteSmsApplication.CHANNEL_FOREGROUND_SILENT)
                .setContentTitle(getString(R.string.foreground_notification_title))
                .setContentText(getString(R.string.foreground_notification_text))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(getString(R.string.foreground_notification_title))
                .setContentText(getString(R.string.foreground_notification_text))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var isRunning = false
            private set
    }
}
