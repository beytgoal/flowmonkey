package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Gemini REST API Request & Response Data Classes ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val thinkingConfig: ThinkingConfig? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    val thinkingLevel: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

// --- AI Video Generation Data Classes ---

@JsonClass(generateAdapter = true)
data class GenerateVideosRequest(
    val prompt: String,
    val config: VideoGenConfig? = null
)

@JsonClass(generateAdapter = true)
data class VideoGenConfig(
    val numberOfVideos: Int = 1,
    val resolution: String = "1080p",
    val aspectRatio: String = "16:9",
    val durationSeconds: Int = 5
)

typealias VeoConfig = VideoGenConfig

@JsonClass(generateAdapter = true)
data class GenerateVideosResponse(
    val name: String? = null,
    val error: Map<String, Any>? = null,
    val response: VideoGenResponseContent? = null
)

@JsonClass(generateAdapter = true)
data class VideoGenResponseContent(
    val generatedVideos: List<VideoGenVideoItem>? = null
)

@JsonClass(generateAdapter = true)
data class VideoGenVideoItem(
    val video: VideoGenVideoData? = null
)

@JsonClass(generateAdapter = true)
data class VideoGenVideoData(
    val uri: String? = null,
    val mimeType: String? = "video/mp4"
)
