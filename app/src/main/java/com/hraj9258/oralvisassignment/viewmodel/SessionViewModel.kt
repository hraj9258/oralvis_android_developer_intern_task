package com.hraj9258.oralvisassignment.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hraj9258.oralvisassignment.data.Session
import com.hraj9258.oralvisassignment.data.SessionState
import com.hraj9258.oralvisassignment.database.AppDatabase
import com.hraj9258.oralvisassignment.repository.SessionRepository
import com.hraj9258.oralvisassignment.storage.ImageStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionViewModel(context: Context) : ViewModel() {

    private val database = AppDatabase.getDatabase(context)
    private val repository = SessionRepository(database.sessionDao())
    private val storageManager = ImageStorageManager(context)

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _capturedImages = MutableStateFlow<List<Bitmap>>(emptyList())
    val capturedImages: StateFlow<List<Bitmap>> = _capturedImages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun startSession() {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val now = Date()
        val formattedDate = dateFormat.format(now)
        val sessionId = "SESSION_${formattedDate}"
//        val sessionId = "SESSION_${System.currentTimeMillis()}"
        _sessionState.value = SessionState.Active(sessionId, 0)
        _capturedImages.value = emptyList()
        _errorMessage.value = null
        Log.d("SessionViewModel", "Started new session: $sessionId")
    }

    fun captureImage(bitmap: Bitmap) {
        val currentState = _sessionState.value
        if (currentState is SessionState.Active) {
            viewModelScope.launch {
                try {
                    _isLoading.value = true

                    // Save image to storage
                    val imagePath = storageManager.saveImage(currentState.sessionId, bitmap)
                    Log.d("SessionViewModel", "Image saved to: $imagePath")

                    // Update UI state
                    _capturedImages.value = _capturedImages.value + bitmap
                    _sessionState.value = currentState.copy(
                        imageCount = currentState.imageCount + 1
                    )

                } catch (e: Exception) {
                    Log.e("SessionViewModel", "Error saving image", e)
                    _errorMessage.value = "Failed to save image: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun endSession(name: String, age: Int) {
        val currentState = _sessionState.value
        if (currentState is SessionState.Active && name.isNotBlank() && age > 0) {
            viewModelScope.launch {
                try {
                    _isLoading.value = true

                    val session = Session(
                        sessionId = currentState.sessionId,
                        name = name.trim(),
                        age = age,
                        timestamp = System.currentTimeMillis(),
                        imageCount = currentState.imageCount
                    )

                    repository.insertSession(session)
                    _sessionState.value = SessionState.Completed(currentState.sessionId)
                    Log.d("SessionViewModel", "Session completed: ${session.sessionId}")

                } catch (e: Exception) {
                    Log.e("SessionViewModel", "Error ending session", e)
                    _errorMessage.value = "Failed to save session: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            _errorMessage.value = "Please provide valid name and age"
        }
    }

    fun resetToIdle() {
        _sessionState.value = SessionState.Idle
        _capturedImages.value = emptyList()
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}