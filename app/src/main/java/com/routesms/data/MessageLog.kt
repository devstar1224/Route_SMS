package com.routesms.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

data class ForwardedMessage(
    val source: String,
    val sender: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val slackResult: Boolean = true
)

object MessageLog {
    private val _messages = MutableStateFlow<List<ForwardedMessage>>(emptyList())
    val messages: StateFlow<List<ForwardedMessage>> = _messages.asStateFlow()

    fun addMessage(msg: ForwardedMessage) {
        _messages.update { current ->
            (listOf(msg) + current).take(50)
        }
    }
}

object DeduplicationCache {
    // sender+content 기반 중복 캐시
    private val contentCache = ConcurrentHashMap<String, Long>()
    // sender만 기반 중복 캐시 (MMS content가 다르게 읽히는 경우 대비)
    private val senderCache = ConcurrentHashMap<String, Long>()
    private const val TTL_MS = 5_000L

    /**
     * sender+content 기반 중복 체크
     * SMSReceiver 내부 중복 방지용
     */
    fun isDuplicate(sender: String, content: String): Boolean {
        cleanExpired()
        val key = buildContentKey(sender, content)
        val now = System.currentTimeMillis()
        val existing = contentCache[key]
        return existing != null && (now - existing) < TTL_MS
    }

    /**
     * sender만으로 중복 체크 (content 무시)
     * NotificationListener에서 사용 - MMS/SMS가 이미 처리됐는지 판별
     */
    fun isDuplicateBySender(sender: String): Boolean {
        cleanExpired()
        val key = normalizeSender(sender)
        val now = System.currentTimeMillis()
        val existing = senderCache[key]
        return existing != null && (now - existing) < TTL_MS
    }

    /**
     * 양쪽 캐시 모두에 등록
     * SMSReceiver에서 호출
     */
    fun register(sender: String, content: String) {
        cleanExpired()
        contentCache[buildContentKey(sender, content)] = System.currentTimeMillis()
        senderCache[normalizeSender(sender)] = System.currentTimeMillis()
    }

    /**
     * content 캐시만 등록 (sender 캐시는 건드리지 않음)
     * NotificationListener에서 RCS 전송 후 호출
     */
    fun registerContent(sender: String, content: String) {
        cleanExpired()
        contentCache[buildContentKey(sender, content)] = System.currentTimeMillis()
    }

    private fun normalizeSender(sender: String): String {
        return sender.replace(Regex("[^0-9+]"), "")
    }

    private fun buildContentKey(sender: String, content: String): String {
        val normalizedSender = normalizeSender(sender)
        return "$normalizedSender:$content".hashCode().toString()
    }

    private fun cleanExpired() {
        val now = System.currentTimeMillis()
        contentCache.entries.removeIf { (now - it.value) >= TTL_MS }
        senderCache.entries.removeIf { (now - it.value) >= TTL_MS }
    }
}
