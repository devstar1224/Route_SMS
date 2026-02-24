package com.routesms.ui.screen

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.routesms.data.FilterRepository
import com.routesms.data.FilterRule
import com.routesms.data.FilterTarget
import com.routesms.data.FilterType
import com.routesms.data.settingsDataStore
import com.routesms.ui.component.AdBanner
import com.routesms.ui.component.FilterRuleDialog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FilterViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.settingsDataStore

    val filters: StateFlow<List<FilterRule>> = FilterRepository.filtersFlow(dataStore)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFilter(rule: FilterRule) {
        viewModelScope.launch {
            val current = filters.value.toMutableList()
            current.add(rule)
            FilterRepository.saveFilters(dataStore, current)
        }
    }

    fun updateFilter(rule: FilterRule) {
        viewModelScope.launch {
            val current = filters.value.toMutableList()
            val index = current.indexOfFirst { it.id == rule.id }
            if (index >= 0) {
                current[index] = rule
                FilterRepository.saveFilters(dataStore, current)
            }
        }
    }

    fun deleteFilter(ruleId: String) {
        viewModelScope.launch {
            val current = filters.value.toMutableList()
            current.removeAll { it.id == ruleId }
            FilterRepository.saveFilters(dataStore, current)
        }
    }

    fun toggleFilter(ruleId: String) {
        viewModelScope.launch {
            val current = filters.value.toMutableList()
            val index = current.indexOfFirst { it.id == ruleId }
            if (index >= 0) {
                current[index] = current[index].copy(isEnabled = !current[index].isEnabled)
                FilterRepository.saveFilters(dataStore, current)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    onNavigateBack: () -> Unit,
    viewModel: FilterViewModel = viewModel()
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<FilterRule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("필터 설정") },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "필터 추가")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { if (com.routesms.AppConfig.SHOW_ADS) AdBanner() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 필터 설명
            Text(
                text = "차단: 등록된 번호/키워드의 메시지를 Slack에 전달하지 않습니다.\n" +
                        "허용: 허용 규칙이 있으면 매칭되는 메시지만 전달됩니다.\n" +
                        "(차단이 허용보다 우선합니다)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (filters.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "필터 규칙이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+ 버튼으로 필터를 추가하세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters, key = { it.id }) { rule ->
                        FilterRuleCard(
                            rule = rule,
                            onToggle = { viewModel.toggleFilter(rule.id) },
                            onDelete = {
                                val deletedRule = rule
                                viewModel.deleteFilter(rule.id)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "필터 삭제됨",
                                        actionLabel = "취소"
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.addFilter(deletedRule)
                                    }
                                }
                            },
                            onEdit = { editingRule = rule }
                        )
                    }
                }
            }
        }
    }

    // 추가 다이얼로그
    if (showAddDialog) {
        FilterRuleDialog(
            onDismiss = { showAddDialog = false },
            onSave = { rule ->
                viewModel.addFilter(rule)
                showAddDialog = false
            }
        )
    }

    // 수정 다이얼로그
    editingRule?.let { rule ->
        FilterRuleDialog(
            existingRule = rule,
            onDismiss = { editingRule = null },
            onSave = { updatedRule ->
                viewModel.updateFilter(updatedRule)
                editingRule = null
            }
        )
    }
}

@Composable
private fun FilterRuleCard(
    rule: FilterRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val typeColor = when (rule.type) {
        FilterType.BLOCK -> Color(0xFFF44336)
        FilterType.ALLOW -> Color(0xFF1565C0)
    }

    val typeLabel = when (rule.type) {
        FilterType.BLOCK -> "차단"
        FilterType.ALLOW -> "허용"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = typeColor.copy(alpha = 0.15f),
                            labelColor = typeColor
                        )
                    )
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = if (rule.target == FilterTarget.PHONE_NUMBER) "전화번호" else "키워드",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rule.pattern,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (rule.isEnabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Switch(
                checked = rule.isEnabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
