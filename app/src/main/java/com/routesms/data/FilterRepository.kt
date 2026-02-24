package com.routesms.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

object FilterRepository {

    private val gson = Gson()

    fun filtersFlow(dataStore: DataStore<Preferences>): Flow<List<FilterRule>> {
        return dataStore.filterRulesJsonFlow().map { json ->
            parseFilters(json)
        }
    }

    suspend fun saveFilters(dataStore: DataStore<Preferences>, filters: List<FilterRule>) {
        val json = gson.toJson(filters)
        dataStore.saveFilterRulesJson(json)
    }

    /**
     * 메시지를 전달해야 하는지 판단합니다.
     *
     * 우선순위:
     * 1. 차단 규칙 (최우선): 매칭되면 무조건 차단
     * 2. 허용 규칙: 허용 규칙이 하나라도 있으면, 매칭되는 메시지만 전달
     * 3. 규칙 없음: 모든 메시지 전달 (기본값)
     *
     * 예시:
     * - 차단만 사용 → 블랙리스트 (차단된 것만 안 감)
     * - 허용만 사용 → 화이트리스트 (허용된 것만 감)
     * - 둘 다 사용 → 차단 우선 체크 후, 허용에 매칭되는 것만 전달
     */
    fun shouldForward(sender: String, content: String, rules: List<FilterRule>): Boolean {
        val activeRules = rules.filter { it.isEnabled }
        if (activeRules.isEmpty()) return true

        val blockRules = activeRules.filter { it.type == FilterType.BLOCK }
        val allowRules = activeRules.filter { it.type == FilterType.ALLOW }

        // 1단계: 차단 규칙 체크 (최우선)
        for (rule in blockRules) {
            if (matchesRule(sender, content, rule)) {
                return false
            }
        }

        // 2단계: 허용 규칙이 있으면 매칭되는 것만 통과
        if (allowRules.isNotEmpty()) {
            return allowRules.any { matchesRule(sender, content, it) }
        }

        // 3단계: 차단에 안 걸리고 허용 규칙이 없으면 통과
        return true
    }

    /**
     * Context에서 동기적으로 필터 규칙을 로드합니다.
     * BroadcastReceiver / Service에서 사용.
     */
    fun loadFiltersSync(context: Context): List<FilterRule> {
        return try {
            val json = runBlocking {
                context.settingsDataStore.filterRulesJsonFlow().first()
            }
            parseFilters(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseFilters(json: String): List<FilterRule> {
        return try {
            val type = object : TypeToken<List<FilterRule>>() {}.type
            gson.fromJson<List<FilterRule>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun matchesRule(sender: String, content: String, rule: FilterRule): Boolean {
        return when (rule.target) {
            FilterTarget.PHONE_NUMBER -> {
                val normalizedSender = sender.replace(Regex("[^0-9+]"), "")
                val normalizedPattern = rule.pattern.replace(Regex("[^0-9+]"), "")
                normalizedSender.contains(normalizedPattern)
            }
            FilterTarget.KEYWORD -> {
                content.contains(rule.pattern, ignoreCase = true)
            }
        }
    }
}
