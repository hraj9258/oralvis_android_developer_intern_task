package com.hraj9258.oralvisassignment.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val sessionId: String,
    val name: String,
    val age: Int,
    val timestamp: Long,
    val imageCount: Int
)