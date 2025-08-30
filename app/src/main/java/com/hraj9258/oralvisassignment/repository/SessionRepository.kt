package com.hraj9258.oralvisassignment.repository

import com.hraj9258.oralvisassignment.data.Session
import com.hraj9258.oralvisassignment.database.SessionDao
import kotlinx.coroutines.flow.Flow

class SessionRepository(
    private val sessionDao: SessionDao
) {
    fun getAllSessions(): Flow<List<Session>> = sessionDao.getAllSessions()

    suspend fun getSessionById(sessionId: String): Session? =
        sessionDao.getSessionById(sessionId)

    suspend fun insertSession(session: Session) =
        sessionDao.insertSession(session)

    suspend fun updateSession(session: Session) =
        sessionDao.updateSession(session)

    suspend fun deleteSession(session: Session) =
        sessionDao.deleteSession(session)

    suspend fun getSessionCount(): Int =
        sessionDao.getSessionCount()
}