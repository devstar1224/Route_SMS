package com.routesms

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.android.gms.ads.MobileAds
import com.routesms.data.MessageLog

class RouteSmsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MessageLog.init(this)
        createNotificationChannels()
        MobileAds.initialize(this) {}
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // 상태바에 아이콘이 보이지 않는 silent 채널
            val foregroundChannel = NotificationChannel(
                CHANNEL_FOREGROUND_SILENT,
                "서비스 상태",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "문자전달 서비스 실행 상태"
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

            // 이전 버전에서 생성된 보이는 채널 정리
            manager.deleteNotificationChannel(CHANNEL_FOREGROUND)
        }
    }

    companion object {
        const val CHANNEL_FOREGROUND = "route_sms_foreground"
        const val CHANNEL_FOREGROUND_SILENT = "route_sms_foreground_silent"
        const val CHANNEL_FORWARDED = "route_sms_forwarded"
    }
}
