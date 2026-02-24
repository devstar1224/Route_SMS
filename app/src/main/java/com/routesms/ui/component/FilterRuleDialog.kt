package com.routesms.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.routesms.data.FilterRule
import com.routesms.data.FilterTarget
import com.routesms.data.FilterType

@Composable
fun FilterRuleDialog(
    existingRule: FilterRule? = null,
    onDismiss: () -> Unit,
    onSave: (FilterRule) -> Unit
) {
    var filterType by remember { mutableStateOf(existingRule?.type ?: FilterType.BLOCK) }
    var filterTarget by remember { mutableStateOf(existingRule?.target ?: FilterTarget.PHONE_NUMBER) }
    var pattern by remember { mutableStateOf(existingRule?.pattern ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = if (existingRule != null) "필터 수정" else "필터 추가",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 필터 타입: 차단 / 허용
                Text(
                    text = "필터 유형",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterType == FilterType.BLOCK,
                        onClick = { filterType = FilterType.BLOCK },
                        label = { Text("차단") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = filterType == FilterType.ALLOW,
                        onClick = { filterType = FilterType.ALLOW },
                        label = { Text("허용") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 필터 대상: 전화번호 / 키워드
                Text(
                    text = "필터 대상",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterTarget == FilterTarget.PHONE_NUMBER,
                        onClick = { filterTarget = FilterTarget.PHONE_NUMBER },
                        label = { Text("전화번호") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = filterTarget == FilterTarget.KEYWORD,
                        onClick = { filterTarget = FilterTarget.KEYWORD },
                        label = { Text("키워드") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 패턴 입력
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = {
                        Text(
                            if (filterTarget == FilterTarget.PHONE_NUMBER) "전화번호"
                            else "키워드"
                        )
                    },
                    placeholder = {
                        Text(
                            if (filterTarget == FilterTarget.PHONE_NUMBER) "010-1234-5678"
                            else "광고"
                        )
                    },
                    supportingText = {
                        Text(
                            when (filterType) {
                                FilterType.BLOCK -> {
                                    if (filterTarget == FilterTarget.PHONE_NUMBER)
                                        "이 번호에서 온 메시지를 Slack에 전달하지 않습니다"
                                    else
                                        "이 키워드가 포함된 메시지를 Slack에 전달하지 않습니다"
                                }
                                FilterType.ALLOW -> {
                                    if (filterTarget == FilterTarget.PHONE_NUMBER)
                                        "이 번호에서 온 메시지만 Slack에 전달합니다"
                                    else
                                        "이 키워드가 포함된 메시지만 Slack에 전달합니다"
                                }
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (pattern.isNotBlank()) {
                                onSave(
                                    FilterRule(
                                        id = existingRule?.id ?: java.util.UUID.randomUUID().toString(),
                                        type = filterType,
                                        target = filterTarget,
                                        pattern = pattern.trim(),
                                        isEnabled = existingRule?.isEnabled ?: true
                                    )
                                )
                            }
                        },
                        enabled = pattern.isNotBlank()
                    ) {
                        Text("저장")
                    }
                }
            }
        }
    }
}
