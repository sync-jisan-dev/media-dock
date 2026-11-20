package com.mediadock.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediadock.app.core.database.entity.HistoryEntity
import com.mediadock.app.domain.usecase.GetHistoryUseCase
import android.net.Uri
import com.mediadock.app.core.storage.StorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val storageManager: StorageManager? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMediaType = MutableStateFlow("ALL")
    val selectedMediaType: StateFlow<String> = _selectedMediaType.asStateFlow()

    val historyItems = getHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun removeItems(items: List<HistoryEntity>) {
        items.forEach { item ->
            runCatching { storageManager?.deleteMediaFile(Uri.parse(item.localUri)) }
            getHistoryUseCase.delete(item.id)
        }
    }
    suspend fun restoreItems(items: List<HistoryEntity>) = getHistoryUseCase.restore(items)

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }

    fun selectFilter(type: String) {
        _selectedMediaType.value = type
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            getHistoryUseCase.delete(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            getHistoryUseCase.clearAll()
        }
    }
}
