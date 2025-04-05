package com.mycelium.myapplication.data.repository

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface AssistantApi {

    @GET("health")
    suspend fun checkHealth(): Response<Map<String, String>>

    @Multipart
    @POST("upload")
    suspend fun uploadAudio(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>

    @Multipart
    @POST("{session_id}/upload-chunk")
    suspend fun uploadChunk(
        @Path("session_id") sessionId: String,
        @Query("chunk_index") chunkIndex: Int,
        @Query("is_last_chunk") isLastChunk: Boolean,
        @Part chunk: MultipartBody.Part
    ): Response<ChunkUploadResponse>

    @POST("{session_id}/analyse")
    suspend fun sessionFinished(
        @Path("session_id") sessionId: String
    )

    @GET("{session_id}/download")
    suspend fun downloadResult(
        @Path("session_id") sessionId: String,
        @Query("type") type: String = "transcript"
    ): Response<ResponseBody>

    @GET("{session_id}/status")
    suspend fun getStatus(
        @Path("session_id") sessionId: String
    ): Map<String, Boolean>

    @GET("types")
    suspend fun getAvailableResultTypes(): List<String>
}

data class UploadResponse(
    val message: String,
    val file_id: String
)

data class ChunkUploadResponse(
    val message: String,
    val session_id: String,
    val chunk_index: Int,
    val is_last_chunk: Boolean
)