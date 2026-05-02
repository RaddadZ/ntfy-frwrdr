package com.playground.android

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registered_apps")
data class RegisteredAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val label: String,
    val emojiTag: String = "",
    val openUrl: String = "",
    val iconUrl: String = "",
    val enabled: Boolean = true
)
