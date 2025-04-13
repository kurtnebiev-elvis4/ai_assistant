package com.mycelium.myapplication.data.repository

import com.mycelium.myapplication.data.db.PromptDao
import com.mycelium.myapplication.data.model.Prompt
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptRepository @Inject constructor(
    private val promptDao: PromptDao
) {
    fun getAllPrompts(): Flow<List<Prompt>> = promptDao.getAllPrompts()
    
    suspend fun insertPrompt(prompt: Prompt): Long = promptDao.insertPrompt(prompt)
    
    suspend fun updatePrompt(prompt: Prompt) = promptDao.updatePrompt(prompt)
    
    suspend fun deletePrompt(prompt: Prompt) = promptDao.deletePrompt(prompt)

    /**
     * Converts the list of prompts to a map format required by the API
     */
    fun promptsToApiMap(prompts: List<Prompt>): Map<String, String> {
        return prompts.associate { it.label to it.message }
    }
}