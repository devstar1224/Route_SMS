package com.routesms.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.routesms.data.DeduplicationCache
import com.routesms.data.FilterRepository
import com.routesms.data.ForwardedMessage
import com.routesms.data.MessageLog
import com.routesms.data.forwardingEnabledFlow
import com.routesms.data.incrementDailyMessageCount
import com.routesms.data.incrementForwardedTotalCount
import com.routesms.data.settingsDataStore
import com.routesms.slack.SlackWebHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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

        // Android가 민감 알림 내용을 숨길 때 표시하는 문구 (전달하지 않음)
        private const val SENSITIVE_CONTENT_HIDDEN = "Sensitive notification content hidden"
    }

    // 서비스가 연결된 시점 — 이 시간 이전에 게시된 알림은 무시
    private var listenerConnectedTime = System.currentTimeMillis()

    override fun onListenerConnected() {
        super.onListenerConnected()
        listenerConnectedTime = System.currentTimeMillis()
        Log.d(TAG, "Listener connected at $listenerConnectedTime")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName !in MESSAGING_PACKAGES) return

        // 서비스 연결 이전에 게시된 알림은 무시 (기존 알림 재처리 방지)
        if (sbn.postTime < listenerConnectedTime) {
            Log.d(TAG, "Old notification ignored (postTime=${sbn.postTime}, connected=$listenerConnectedTime): ${sbn.packageName}")
            return
        }

        try {
            val extras = sbn.notification.extras ?: return
            val sender = extras.getString(Notification.EXTRA_TITLE) ?: return
            val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

            // Android가 민감 콘텐츠를 숨긴 경우 무시
            if (content == SENSITIVE_CONTENT_HIDDEN) {
                Log.d(TAG, "Sensitive content hidden by OS, skipped: $sender")
                return
            }

            // SMS/MMS는 SMSReceiver가 먼저 처리하므로, 대기 후 중복 체크
            // SMSReceiver가 register()할 시간을 확보한 뒤 isDuplicate()로 판별
            CoroutineScope(Dispatchers.IO).launch {
                delay(DEDUP_WAIT_MS)

                // 전달 비활성화 시 무시
                val forwardingEnabled = applicationContext.settingsDataStore
                    .forwardingEnabledFlow().first()
                if (!forwardingEnabled) {
                    Log.d(TAG, "Forwarding disabled, skipped RCS: $sender")
                    return@launch
                }

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

                // content만으로 체크 (알림 sender가 이름, SMS sender가 번호인 경우)
                if (DeduplicationCache.isDuplicateByContent(content)) {
                    Log.d(TAG, "Already handled (content match), skipped: $sender")
                    return@launch
                }

                // 여기 도달 = SMSReceiver가 처리하지 못한 메시지 (RCS 등)
                DeduplicationCache.registerContent(sender, content)

                // 필터 규칙 체크
                val filters = FilterRepository.loadFiltersSync(applicationContext)
                if (!FilterRepository.shouldForward(sender, content, filters)) {
                    Log.d(TAG, "Filtered out RCS: $sender")
                    MessageLog.addMessage(
                        ForwardedMessage(
                            source = "RCS",
                            sender = sender,
                            content = content,
                            blocked = true
                        )
                    )
                    return@launch
                }

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

                // 일일/총 전송 카운터 증가
                applicationContext.settingsDataStore.incrementDailyMessageCount()
                applicationContext.settingsDataStore.incrementForwardedTotalCount()

                Log.d(TAG, "RCS message forwarded: $sender")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }
}
