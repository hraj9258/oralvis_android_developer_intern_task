package com.hraj9258.oralvisassignment.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hraj9258.oralvisassignment.data.Session
import com.hraj9258.oralvisassignment.database.AppDatabase
import com.hraj9258.oralvisassignment.repository.SessionRepository
import com.hraj9258.oralvisassignment.storage.ImageStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SearchViewModel(context: Context) : ViewModel() {

    private val database = AppDatabase.getDatabase(context)
    private val repository = SessionRepository(database.sessionDao())
    private val storageManager = ImageStorageManager(context)

    private val _allSessions = MutableStateFlow<List<Session>>(emptyList())
    val allSessions: StateFlow<List<Session>> = _allSessions.asStateFlow()

    private val _searchedSession = MutableStateFlow<Session?>(null)
    val searchedSession: StateFlow<Session?> = _searchedSession.asStateFlow()

    private val _sessionImages = MutableStateFlow<List<File>>(emptyList())
    val sessionImages: StateFlow<List<File>> = _sessionImages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadAllSessions()
    }

    private fun loadAllSessions() {
        viewModelScope.launch {
            repository.getAllSessions().collect { sessions ->
                _allSessions.value = sessions
            }
        }
    }

    fun searchSession(sessionId: String) {
        if (sessionId.isBlank()) {
            _errorMessage.value = "Please enter a Session ID"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val session = repository.getSessionById(sessionId.trim())
                if (session != null) {
                    _searchedSession.value = session
                    loadSessionImages(session.sessionId)
                } else {
                    _searchedSession.value = null
                    _sessionImages.value = emptyList()
                    _errorMessage.value = "Session not found: $sessionId"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error searching session: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSessionDetails(sessionId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val session = repository.getSessionById(sessionId)
                if (session != null) {
                    _searchedSession.value = session
                    loadSessionImages(sessionId)
                } else {
                    _errorMessage.value = "Session not found"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error loading session: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadSessionImages(sessionId: String) {
        try {
            val images = storageManager.getSessionImages(sessionId)
            _sessionImages.value = images
        } catch (e: Exception) {
            _errorMessage.value = "Error loading images: ${e.message}"
        }
    }

    fun clearSearch() {
        _searchedSession.value = null
        _sessionImages.value = emptyList()
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}