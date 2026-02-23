package com.routesms.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.routesms.data.DeduplicationCache
import com.routesms.data.ForwardedMessage
import com.routesms.data.MessageLog
import com.routesms.slack.SlackWebHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationListenerSvc : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListenerSvc"
        private val MESSAGING_PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.lge.message",
            "com.sonyericsson.conversations",
        )
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)

        // SMSReceiver가 처리할 수 있도록 대기하는 시간
        // SMSReceiver가 먼저 register() → 여기서 isDuplicate()로 걸러짐
        private const val DEDUP_WAIT_MS = 3000L
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName !in MESSAGING_PACKAGES) return

        try {
            val extras = sbn.notification.extras ?: return
            val sender = extras.getString(Notification.EXTRA_TITLE) ?: return
            val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

            // SMS/MMS는 SMSReceiver가 먼저 처리하므로, 대기 후 중복 체크
            // SMSReceiver가 register()할 시간을 확보한 뒤 isDuplicate()로 판별
            CoroutineScope(Dispatchers.IO).launch {
                delay(DEDUP_WAIT_MS)

                // sender+content 일치 체크 (SMS 동일 내용)
                if (DeduplicationCache.isDuplicate(sender, content)) {
                    Log.d(TAG, "Already handled (content match), skipped: $sender")
                    return@launch
                }

                // sender만으로 체크 (MMS는 content가 다르게 읽힐 수 있음)
                if (DeduplicationCache.isDuplicateBySender(sender)) {
                    Log.d(TAG, "Already handled (sender match), skipped: $sender")
                    return@launch
                }

                // 여기 도달 = SMSReceiver가 처리하지 못한 메시지 (RCS 등)
                DeduplicationCache.registerContent(sender, content)

                val dateStr = dateFormat.format(Date())

                SlackWebHook.builder()
                    .title("메시지 알림 수신")
                    .timeStampEnabled(true)
                    .color("#00CC00")
                    .fields("발신" to sender, "내용" to content, "수신시각" to dateStr)
                    .build()
                    .send(applicationContext)

                MessageLog.addMessage(
                    ForwardedMessage(
                        source = "RCS",
                        sender = sender,
                        content = content
                    )
                )

                Log.d(TAG, "RCS message forwarded: $sender")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }
}
