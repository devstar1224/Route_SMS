package com.routesms.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.routesms.R

@Composable
fun SlackConnectionCard(
    isWebhookConfigured: Boolean,
    modifier: Modifier = Modifier
) {
    var showGuide by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 헤더
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_slack),
                    contentDescription = "Slack",
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Slack Webhook 연동",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Webhook 상태
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isWebhookConfigured) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (isWebhookConfigured) Color(0xFF4CAF50) else Color(0xFFFFA000),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isWebhookConfigured) "Webhook 설정 완료" else "Webhook URL 설정 필요",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isWebhookConfigured) Color(0xFF4CAF50) else Color(0xFFFFA000)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 가이드 버튼
            OutlinedButton(
                onClick = { showGuide = !showGuide }
            ) {
                Text("Webhook 발급 가이드")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (showGuide) Icons.Outlined.KeyboardArrowUp
                    else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Webhook 가이드
            AnimatedVisibility(visible = showGuide) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Webhook URL 발급 방법",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val steps = listOf(
                        "1. Slack 앱 또는 웹에서 워크스페이스에 접속",
                        "2. 설정 > 앱 관리 > Incoming Webhooks 검색",
                        "3. 채널을 선택하고 Webhook URL 발급",
                        "4. URL에서 hooks.slack.com/ 뒤의 경로만 복사",
                        "5. 설정 화면에서 경로를 입력하고 저장"
                    )
                    steps.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
