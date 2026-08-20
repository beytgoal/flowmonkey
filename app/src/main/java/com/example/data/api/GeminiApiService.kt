package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {

    // Standard call (used for gemini-3.5-flash and gemini-3.1-pro-preview)
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    // Veo 3 Video Generation call
    @POST("v1beta/models/{model}:generateVideos")
    suspend fun generateVideos(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateVideosRequest
    ): GenerateVideosResponse
}

object ApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Core default Omni Generative Multi-modal Models
    const val DEFAULT_OMNI_MULTIMODAL_MODEL = "gemini-3.5-flash"
    const val DEFAULT_OMNI_THINKING_MODEL = "gemini-3.1-pro-preview"
    const val DEFAULT_OMNI_VIDEO_MODEL = "veo-3.1-fast-generate-preview"

    @Volatile
    private var directedUserApiKey: String? = null

    fun setUserApiKey(key: String?) {
        directedUserApiKey = key?.trim()?.takeIf { it.isNotBlank() }
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val geminiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    fun getApiKey(): String {
        val userKey = directedUserApiKey
        if (!userKey.isNullOrBlank()) {
            return userKey
        }
        return BuildConfig.GEMINI_API_KEY
    }
}
