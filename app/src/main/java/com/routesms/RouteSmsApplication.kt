package com.routesms

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class RouteSmsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val foregroundChannel = NotificationChannel(
                CHANNEL_FOREGROUND,
                "서비스 상태",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SMS 라우팅 서비스 실행 상태"
                setShowBadge(false)
            }

            val forwardedChannel = NotificationChannel(
                CHANNEL_FORWARDED,
                "전달된 메시지",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Slack으로 전달된 메시지 알림"
            }

            manager.createNotificationChannel(foregroundChannel)
            manager.createNotificationChannel(forwardedChannel)
        }
    }

    companion object {
        const val CHANNEL_FOREGROUND = "route_sms_foreground"
        const val CHANNEL_FORWARDED = "route_sms_forwarded"
    }
}
