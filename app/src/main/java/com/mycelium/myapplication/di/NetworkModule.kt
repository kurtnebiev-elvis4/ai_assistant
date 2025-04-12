package com.mycelium.myapplication.di

import android.content.Context
import android.content.SharedPreferences
import com.mycelium.myapplication.data.model.ServerEntry
import com.mycelium.myapplication.data.repository.AssistantApi
import com.mycelium.myapplication.data.repository.ChatRepository
import com.mycelium.myapplication.data.repository.ChatRepositoryImpl
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
            name = "Primary Server",
            runpodId = "1k1ju5t9x5447q"
        ),
        ServerEntry(
            name = "Backup Server 1",
            runpodId = "uezrdr6qo056jf"
        ),
        ServerEntry(
            name = "Backup Server 2",
            runpodId = "909qwv684lqjq7"
        ),
        ServerEntry(
            name = "Backup Server 3",
            runpodId = "pn8vl8jb7ugske"
        ),
        ServerEntry(
            name = "Backup Server 4",
            runpodId = "u9xt9rn2ib2p1i"
        ),
        ServerEntry(
            name = "Backup Server 5",
            runpodId = "e1kool62n24bws"
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
            val existingServers = serverManager.serverSet
            defaultServers.forEach { server ->
                if(existingServers.contains(server)) return@forEach
                val serverUrl = serverManager.addCustomServer(
                    name = server.name,
                    runpodId = server.runpodId,
                    port = server.port
                ).serverUrl

                // Select the first server as default
                if (server == defaultServers.first()) {
                    serverManager.selectServer(serverUrl)
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
            // Wait for the server list to be loaded and selected server to be initialized
            val selected = serverManager.selectedServer.first()
            if (selected != null) {
                selected
            } else {
                // Fallback to the first default server if no server is selected
                val defaultServer = defaultServers.first()
                val serverUrl = serverManager.addCustomServer(
                    name = defaultServer.name,
                    runpodId = defaultServer.runpodId,
                    port = defaultServer.port
                ).serverUrl
                serverManager.selectServer(serverUrl)
                serverManager.selectedServer.first()!!
            }
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(selectedServer.serverUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(AssistantApi::class.java)
    }

    @Provides
    @Singleton
    fun provideChatRepository(client: OkHttpClient): ChatRepository {
        return ChatRepositoryImpl(client)
    }
}
