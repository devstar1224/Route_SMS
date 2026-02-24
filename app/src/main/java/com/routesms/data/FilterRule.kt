package com.routesms.data

import java.util.UUID

data class FilterRule(
    val id: String = UUID.randomUUID().toString(),
    val type: FilterType,
    val target: FilterTarget,
    val pattern: String,
    val isEnabled: Boolean = true
)

enum class FilterType {
    BLOCK,   // 차단: 매칭되면 Slack 전달 안 함 (최우선)
    ALLOW    // 허용: 허용 규칙이 있으면 매칭되는 메시지만 전달
}

enum class FilterTarget {
    PHONE_NUMBER,  // 전화번호
    KEYWORD        // 키워드
}
