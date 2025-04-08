package com.mycelium.myapplication.di

import com.mycelium.myapplication.data.repository.AssistantApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideAssistantApi(): AssistantApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val runpodId = "uezrdr6qo056jf"
//        val runpodId = "909qwv684lqjq7"
//        val runpodId = "pn8vl8jb7ugske"
//        val runpodId = "u9xt9rn2ib2p1i"
        val port = 8000
        val server = "https://$runpodId-$port.proxy.runpod.net/"
        val retrofit = Retrofit.Builder()
            .baseUrl(server)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(AssistantApi::class.java)
    }
}
