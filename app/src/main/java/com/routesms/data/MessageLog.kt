package com.routesms.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class ForwardedMessage(
    val source: String,
    val sender: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val slackResult: Boolean = true,
    val blocked: Boolean = false
)

object MessageLog {
    private const val PREFS_NAME = "message_log"
    private const val KEY_MESSAGES = "messages"
    private const val MAX_MESSAGES = 50

    private val _messages = MutableStateFlow<List<ForwardedMessage>>(emptyList())
    val messages: StateFlow<List<ForwardedMessage>> = _messages.asStateFlow()

    private var prefs: SharedPreferences? = null

    /**
     * Application.onCreate()에서 호출하여 초기화
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _messages.value = loadFromPrefs()
    }

    fun addMessage(msg: ForwardedMessage) {
        _messages.update { current ->
            val updated = (listOf(msg) + current).take(MAX_MESSAGES)
            saveToPrefs(updated)
            updated
        }
    }

    private fun saveToPrefs(messages: List<ForwardedMessage>) {
        val jsonArray = JSONArray()
        messages.forEach { msg ->
            jsonArray.put(JSONObject().apply {
                put("source", msg.source)
                put("sender", msg.sender)
                put("content", msg.content)
                put("timestamp", msg.timestamp)
                put("slackResult", msg.slackResult)
                put("blocked", msg.blocked)
            })
        }
        prefs?.edit()?.putString(KEY_MESSAGES, jsonArray.toString())?.apply()
    }

    private fun loadFromPrefs(): List<ForwardedMessage> {
        val json = prefs?.getString(KEY_MESSAGES, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                ForwardedMessage(
                    source = obj.getString("source"),
                    sender = obj.getString("sender"),
                    content = obj.getString("content"),
                    timestamp = obj.getLong("timestamp"),
                    slackResult = obj.optBoolean("slackResult", true),
                    blocked = obj.optBoolean("blocked", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

object DeduplicationCache {
    // content 해시 기반 중복 캐시
    private val contentCache = ConcurrentHashMap<String, Long>()
    // sender 기반 중복 캐시
    private val senderCache = ConcurrentHashMap<String, Long>()
    // content만으로 (sender 무관) 중복 체크하는 캐시
    private val contentOnlyCache = ConcurrentHashMap<String, Long>()
    private const val TTL_MS = 10_000L

    /**
     * sender+content 기반 중복 체크
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
     * NotificationListener에서 사용
     */
    fun isDuplicateBySender(sender: String): Boolean {
        cleanExpired()
        val key = normalizeSender(sender)
        if (key.isEmpty()) return false
        val now = System.currentTimeMillis()
        val existing = senderCache[key]
        return existing != null && (now - existing) < TTL_MS
    }

    /**
     * content만으로 중복 체크 (sender 무관)
     * 알림의 sender가 이름일 때, SMS의 sender가 번호일 때 대비
     */
    fun isDuplicateByContent(content: String): Boolean {
        cleanExpired()
        val key = normalizeContent(content)
        if (key.isEmpty()) return false
        val now = System.currentTimeMillis()
        val existing = contentOnlyCache[key]
        return existing != null && (now - existing) < TTL_MS
    }

    /**
     * 모든 캐시에 등록
     * SMSReceiver에서 호출
     */
    fun register(sender: String, content: String) {
        val now = System.currentTimeMillis()
        cleanExpired()
        contentCache[buildContentKey(sender, content)] = now
        val normalizedSender = normalizeSender(sender)
        if (normalizedSender.isNotEmpty()) {
            senderCache[normalizedSender] = now
        }
        contentOnlyCache[normalizeContent(content)] = now
    }

    /**
     * content 캐시만 등록
     * NotificationListener에서 RCS 전송 후 호출
     */
    fun registerContent(sender: String, content: String) {
        val now = System.currentTimeMillis()
        cleanExpired()
        contentCache[buildContentKey(sender, content)] = now
        contentOnlyCache[normalizeContent(content)] = now
    }

    private fun normalizeSender(sender: String): String {
        return sender.replace(Regex("[^0-9+]"), "")
    }

    private fun normalizeContent(content: String): String {
        return content.trim().replace(Regex("\\s+"), " ")
    }

    private fun buildContentKey(sender: String, content: String): String {
        val normalizedSender = normalizeSender(sender)
        val normalizedContent = normalizeContent(content)
        return "$normalizedSender:$normalizedContent".hashCode().toString()
    }

    private fun cleanExpired() {
        val now = System.currentTimeMillis()
        contentCache.entries.removeIf { (now - it.value) >= TTL_MS }
        senderCache.entries.removeIf { (now - it.value) >= TTL_MS }
        contentOnlyCache.entries.removeIf { (now - it.value) >= TTL_MS }
    }
}
