package com.mycelium.myapplication.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mycelium.myapplication.data.model.Prompt
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts ORDER BY label")
    fun getAllPrompts(): Flow<List<Prompt>>

    @Query("SELECT * FROM prompts WHERE isDefault = 1 ORDER BY label")
    fun getDefaultPrompts(): Flow<List<Prompt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: Prompt): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompts(prompts: List<Prompt>)

    @Update
    suspend fun updatePrompt(prompt: Prompt)

    @Delete
    suspend fun deletePrompt(prompt: Prompt)

    @Query("SELECT * FROM prompts WHERE id = :promptId")
    suspend fun getPromptById(promptId: String): Prompt?
}