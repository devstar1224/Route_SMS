package com.routesms.ui.screen

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.routesms.data.ForwardedMessage
import com.routesms.data.MessageLog
import com.routesms.data.dailyMessageCountFlow
import com.routesms.data.forwardedTotalCountFlow
import com.routesms.data.lastCountResetDateFlow
import com.routesms.data.resetDailyMessageCount
import com.routesms.data.saveLastCountResetDate
import com.routesms.data.settingsDataStore
import com.routesms.data.webhookUrlFlow
import com.routesms.ui.component.AdBanner
import com.routesms.ui.component.DashboardHeroCard
import com.routesms.ui.component.MessageLogList
import com.routesms.ui.component.SlackConnectionCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

    val todayCount: StateFlow<Int> = dataStore.dailyMessageCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCount: StateFlow<Int> = dataStore.forwardedTotalCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        checkDailyReset()
    }

    private fun checkDailyReset() {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val lastReset = dataStore.lastCountResetDateFlow().first()
            if (lastReset != today) {
                dataStore.resetDailyMessageCount()
                dataStore.saveLastCountResetDate(today)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToFilters: () -> Unit = {},
    viewModel: MainViewModel = viewModel()
) {
    val savedUrl by viewModel.webhookUrl.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val todayCount by viewModel.todayCount.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("문자전달") },
                actions = {
                    IconButton(onClick = onNavigateToFilters) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "필터",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "설정",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = { if (com.routesms.AppConfig.SHOW_ADS) AdBanner() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Dashboard Hero Card
            DashboardHeroCard(
                todayCount = todayCount,
                totalCount = totalCount
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Webhook 미설정 시 Slack 연동 카드 표시
            if (savedUrl.isBlank()) {
                SlackConnectionCard(
                    isWebhookConfigured = false
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 메시지 로그 헤더
            Text(
                text = "메시지 기록",
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
