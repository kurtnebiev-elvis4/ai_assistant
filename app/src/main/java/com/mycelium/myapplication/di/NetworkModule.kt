package com.mycelium.myapplication.di

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mycelium.myapplication.data.model.ServerEntry
import com.mycelium.myapplication.data.repository.AssistantApi
import com.mycelium.myapplication.data.repository.ServerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val PREF_NAME = "server_preferences"
private const val PREF_SERVER_LIST = "server_list"
private const val PREF_SELECTED_SERVER_ID = "selected_server_id"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private val defaultServers = listOf(
        ServerEntry(
            id = UUID.randomUUID().toString(),
            name = "Primary Server",
            runpodId = "1k1ju5t9x5447q"
        ),
        ServerEntry(
            id = UUID.randomUUID().toString(),
            name = "Backup Server 1",
            runpodId = "uezrdr6qo056jf"
        ),
        ServerEntry(
            id = UUID.randomUUID().toString(),
            name = "Backup Server 2",
            runpodId = "909qwv684lqjq7"
        ),
        ServerEntry(
            id = UUID.randomUUID().toString(),
            name = "Backup Server 3", 
            runpodId = "pn8vl8jb7ugske"
        ),
        ServerEntry(
            id = UUID.randomUUID().toString(),
            name = "Backup Server 4",
            runpodId = "u9xt9rn2ib2p1i"
        )
    )
    
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    @Provides
    @Singleton
    fun provideServerManager(preferences: SharedPreferences): ServerManager {
        val serverManager = ServerManager(preferences)
        
        // Initialize with default servers if empty
        runBlocking {
            if (serverManager.serverList.first().isEmpty()) {
                // Add default servers
                defaultServers.forEach { server ->
                    val id = serverManager.addCustomServer(
                        name = server.name,
                        runpodId = server.runpodId,
                        port = server.port
                    ).id
                    
                    // Select the first server as default
                    if (server == defaultServers.first()) {
                        serverManager.selectServer(id)
                    }
                }
            }
        }
        
        return serverManager
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }
    
    @Provides
    fun provideServerFactory(
        client: OkHttpClient,
        serverManager: ServerManager
    ): AssistantApi {
        // Get the selected server
        val selectedServer = runBlocking {
            serverManager.selectedServer.first() ?: defaultServers.first()
        }
        
        val retrofit = Retrofit.Builder()
            .baseUrl(selectedServer.serverUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(AssistantApi::class.java)
    }
}
