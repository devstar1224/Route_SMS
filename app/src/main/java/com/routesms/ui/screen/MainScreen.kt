package com.routesms.ui.screen

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.routesms.data.ForwardedMessage
import com.routesms.data.MessageLog
import com.routesms.data.saveWebhookUrl
import com.routesms.data.settingsDataStore
import com.routesms.data.webhookUrlFlow
import com.routesms.service.NotificationListenerSvc
import com.routesms.slack.SlackWebHook
import com.routesms.ui.component.MessageLogList
import com.routesms.ui.component.StatusIndicator
import com.routesms.ui.component.WebhookUrlInput
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.settingsDataStore

    val webhookUrl: StateFlow<String> = dataStore.webhookUrlFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val messages: StateFlow<List<ForwardedMessage>> = MessageLog.messages

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
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val savedUrl by viewModel.webhookUrl.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()

    var urlInput by remember(savedUrl) { mutableStateOf(savedUrl) }
    var notificationListenerEnabled by remember {
        mutableStateOf(viewModel.isNotificationListenerEnabled())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RouteSMS") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
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

            Spacer(modifier = Modifier.height(16.dp))

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

            Text(
                text = "최근 전달 메시지",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            MessageLogList(
                messages = messages,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
