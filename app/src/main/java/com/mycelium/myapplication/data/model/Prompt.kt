package com.mycelium.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "prompts")
data class Prompt(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val message: String,
    val isDefault: Boolean = false
)