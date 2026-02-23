package com.routesms.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WebhookUrlInput(
    url: String,
    onUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text("Webhook 경로") },
            placeholder = { Text("services/T.../B.../...") },
            supportingText = { Text("https://hooks.slack.com/ 뒤의 경로를 입력하세요") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(onClick = onTest) {
                Text("전송 테스트")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onSave) {
                Text("저장")
            }
        }
    }
}
