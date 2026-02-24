package com.routesms.ui.screen

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.routesms.BuildConfig
import com.routesms.data.ForwardedMessage
import com.routesms.data.MessageLog
import com.routesms.data.forwardingEnabledFlow
import com.routesms.data.saveForwardingEnabled
import com.routesms.data.saveWebhookUrl
import com.routesms.data.settingsDataStore
import com.routesms.data.webhookUrlFlow
import com.routesms.service.NotificationListenerSvc
import com.routesms.slack.SlackWebHook
import com.routesms.ui.component.AdBanner
import com.routesms.ui.component.SlackConnectionCard
import com.routesms.ui.component.StatusIndicator
import com.routesms.ui.component.WebhookUrlInput
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.settingsDataStore

    val webhookUrl: StateFlow<String> = dataStore.webhookUrlFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val forwardingEnabled: StateFlow<Boolean> = dataStore.forwardingEnabledFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setForwardingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.saveForwardingEnabled(enabled)
        }
    }

    fun saveWebhookUrl(url: String) {
        viewModelScope.launch {
            dataStore.saveWebhookUrl(url)
        }
    }

    fun sendTestMessage() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
        SlackWebHook.builder()
            .title("수신테스트")
            .timeStampEnabled(true)
            .color("#FF0000")
            .fields(
                "발신" to "0123456789",
                "내용" to "수신 테스트",
                "수신시각" to dateFormat.format(Date())
            )
            .build()
            .send(getApplication())

        MessageLog.addMessage(
            ForwardedMessage(
                source = "TEST",
                sender = "0123456789",
                content = "수신 테스트"
            )
        )
    }

    fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(getApplication<Application>(), NotificationListenerSvc::class.java)
        val enabledListeners = Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(cn.flattenToString()) == true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val savedUrl by viewModel.webhookUrl.collectAsStateWithLifecycle()
    val forwardingEnabled by viewModel.forwardingEnabled.collectAsStateWithLifecycle()

    var urlInput by remember(savedUrl) { mutableStateOf(savedUrl) }
    var notificationListenerEnabled by remember {
        mutableStateOf(viewModel.isNotificationListenerEnabled())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { if (com.routesms.AppConfig.SHOW_ADS) AdBanner() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ── Slack 연동 ──
            SlackConnectionCard(
                isWebhookConfigured = savedUrl.isNotBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            WebhookUrlInput(
                url = urlInput,
                onUrlChange = { urlInput = it },
                onSave = {
                    viewModel.saveWebhookUrl(urlInput)
                    scope.launch { snackbarHostState.showSnackbar("저장완료!") }
                },
                onTest = {
                    viewModel.sendTestMessage()
                    scope.launch { snackbarHostState.showSnackbar("테스트 전송!") }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── 서비스 상태 ──
            StatusIndicator(
                isForegroundServiceRunning = true,
                isNotificationListenerEnabled = notificationListenerEnabled,
                onOpenNotificationSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 설정 ──
            SettingsToggleRow(
                title = "Slack 전달",
                description = if (forwardingEnabled) "수신 문자를 Slack으로 전달 중"
                              else "전달 일시 중지됨",
                checked = forwardingEnabled,
                onCheckedChange = { viewModel.setForwardingEnabled(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── 앱 정보 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp, vertical = 4.dp
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        "오픈소스 라이선스",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
