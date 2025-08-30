package com.hraj9258.oralvisassignment.data

sealed class SessionState {
    object Idle : SessionState()
    data class Active(val sessionId: String, val imageCount: Int) : SessionState()
    data class Completed(val sessionId: String) : SessionState()
}