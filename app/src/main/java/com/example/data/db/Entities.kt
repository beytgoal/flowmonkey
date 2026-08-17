package com.example.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "video_projects")
data class VideoProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val aspectRatio: String = "16:9", // "16:9", "9:16", "1:1"
    val visualStyle: String = "Cinematic",
    val exportResolution: String = "1080p", // "720p", "1080p", "4K"
    val fps: Int = 30, // 24, 30, 60
    val durationSeconds: Int = 15,
    val durationMs: Long = 15000L,
    val localFilePath: String? = null, // Local storage file path for exported/rendered project video
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isTemplate: Boolean = false
)

@Entity(
    tableName = "generated_video_segments",
    foreignKeys = [
        ForeignKey(
            entity = VideoProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class GeneratedVideoSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val segmentIndex: Int = 0,
    val title: String,
    val durationSeconds: Int = 5,
    val durationMs: Long = 5000L,
    val localFilePath: String = "", // Local disk/internal storage file path for generated video segment
    val mediaUri: String = "",
    val prompt: String = "",
    val visualStyle: String = "Cinematic",
    val resolution: String = "1080p",
    val aspectRatio: String = "16:9",
    val fileSizeBytes: Long = 0L,
    val status: String = "READY", // "PENDING", "GENERATING", "READY", "ERROR"
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class VideoProjectWithSegments(
    @Embedded val project: VideoProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val segments: List<GeneratedVideoSegmentEntity>
)

@Entity(
    tableName = "storyboard_scenes",
    foreignKeys = [
        ForeignKey(
            entity = VideoProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class StoryboardSceneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val sceneIndex: Int,
    val title: String,
    val scriptText: String,
    val visualPrompt: String,
    val cameraMovement: String = "Pan Right",
    val durationSeconds: Int = 5,
    val generatedVideoUri: String? = null,
    val sourceImageUri: String? = null,
    val status: String = "DRAFT" // "DRAFT", "GENERATING", "READY", "ERROR"
)

@Entity(
    tableName = "timeline_tracks",
    foreignKeys = [
        ForeignKey(
            entity = VideoProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class TimelineTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val trackType: String, // "VIDEO", "TEXT", "AUDIO", "EFFECT"
    val trackName: String,
    val trackIndex: Int
)

@Entity(
    tableName = "timeline_clips",
    foreignKeys = [
        ForeignKey(
            entity = TimelineTrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId"), Index("projectId")]
)
data class TimelineClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val projectId: Long,
    val title: String,
    val mediaUri: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val speedMultiplier: Float = 1.0f,
    val speedCurve: String = "Normal",
    val filterName: String = "None",
    val transitionType: String = "Fade",
    val textContent: String? = null,
    val volume: Float = 1.0f,
    val animationIn: String = "None",
    val animationOut: String = "None",
    val animationCombo: String = "None",
    val cropRatio: String = "16:9",
    val rotationDegrees: Int = 0,
    val isMirrored: Boolean = false,
    val isReversed: Boolean = false,
    val isFrozen: Boolean = false,
    val effectName: String = "None",
    val bodyEffectName: String = "None",
    val brightness: Float = 0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val temperature: Float = 0f,
    val cutoutMode: String = "None",
    val maskType: String = "None",
    val hasKeyframe: Boolean = false,
    val keyframeData: String = "",
    val stabilizeLevel: String = "None",
    val audioSfx: String = "None",
    val isVoiceover: Boolean = false,
    val stickerIcon: String = "None",
    val vignette: Float = 0f,
    val sharpen: Float = 0f,
    val tint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val audioFadeInSec: Float = 0f,
    val audioFadeOutSec: Float = 0f,
    val audioPitch: Float = 1.0f,
    val noiseReduction: Boolean = false,
    val vocalEnhance: Boolean = false,
    val proxyUri: String = "",
    val proxyStatus: String = "IDLE",
    val opacity: Float = 1.0f,
    val blendMode: String = "Normal",
    val fontFamily: String = "Inter",
    val fontSize: Int = 24,
    val fontColor: String = "#FFFFFF",
    val textAlignment: String = "CENTER"
)
