package com.example.media

import android.content.Context
import android.graphics.*
import android.media.*
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.util.LruCache
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer

data class ImportedMediaResult(
    val permanentFilePath: String,
    val fileName: String,
    val category: String, // "VIDEO", "AUDIO", "IMAGE", "LUT"
    val durationMs: Long,
    val durationText: String,
    val width: Int,
    val height: Int,
    val resolutionOrType: String,
    val thumbnailPath: String?
)

object RealMediaManager {
    private const val TAG = "RealMediaManager"

    // Memory LRU Cache for decoded video frames (Max 64MB of Bitmap memory)
    private val frameCache = object : LruCache<String, Bitmap>(64 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    /**
     * Obtains a secure, shareable content URI via Android FileProvider.
     */
    fun getShareableUri(context: Context, file: File): Uri {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.w(TAG, "FileProvider failed for ${file.absolutePath}, falling back to Uri.fromFile: ${e.message}")
            Uri.fromFile(file)
        }
    }

    /**
     * Imports a user-selected URI from Gallery / File picker / SAF into the app's permanent internal storage.
     * Extracts true duration, resolution, category, and generates a cached keyframe thumbnail.
     */
    suspend fun importMediaFromUri(
        context: Context,
        sourceUri: Uri,
        customTitle: String? = null
    ): ImportedMediaResult = withContext(Dispatchers.IO) {
        var originalFileName = customTitle ?: "media_${System.currentTimeMillis()}"
        var mimeType: String? = null

        try {
            mimeType = context.contentResolver.getType(sourceUri)
            context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        originalFileName = name
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed querying content resolver for name: ${e.message}")
        }

        val extension = originalFileName.substringAfterLast('.', "").lowercase()
        val detectedCategory = when {
            extension in listOf("cube", "3dl", "look") || mimeType?.contains("lut", ignoreCase = true) == true -> "LUT"
            mimeType?.startsWith("video/") == true || extension in listOf("mp4", "mkv", "mov", "webm", "avi", "3gp", "m4v") -> "VIDEO"
            mimeType?.startsWith("audio/") == true || extension in listOf("mp3", "wav", "aac", "ogg", "flac", "m4a", "wma") -> "AUDIO"
            mimeType?.startsWith("image/") == true || extension in listOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp") -> "IMAGE"
            else -> "VIDEO"
        }

        // Create permanent destination file inside app internal directory
        val mediaDir = File(context.filesDir, "imported_media").apply { mkdirs() }
        val cleanName = originalFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val destFile = File(mediaDir, "${System.currentTimeMillis()}_$cleanName")

        try {
            val inputStream: InputStream? = if (sourceUri.scheme == "file") {
                File(sourceUri.path ?: "").inputStream()
            } else {
                context.contentResolver.openInputStream(sourceUri)
            }

            inputStream?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying media file to internal storage: ${e.message}", e)
        }

        val permanentPath = destFile.absolutePath

        // Extract real metadata via MediaMetadataRetriever
        var actualDurationMs = 5000L
        var videoWidth = 1920
        var videoHeight = 1080
        var thumbnailPath: String? = null

        if (detectedCategory == "VIDEO" || detectedCategory == "AUDIO") {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(permanentPath)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (!durationStr.isNullOrEmpty()) {
                    val parsed = durationStr.toLongOrNull() ?: 5000L
                    if (parsed > 0) {
                        actualDurationMs = parsed
                    }
                }

                if (detectedCategory == "VIDEO") {
                    val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

                    var w = widthStr?.toIntOrNull() ?: 1920
                    var h = heightStr?.toIntOrNull() ?: 1080
                    val rot = rotationStr?.toIntOrNull() ?: 0
                    if (rot == 90 || rot == 270) {
                        val temp = w
                        w = h
                        h = temp
                    }
                    videoWidth = w
                    videoHeight = h

                    // Extract thumbnail frame
                    val firstFrame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.getFrameAtTime()
                    if (firstFrame != null) {
                        val thumbDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
                        val thumbFile = File(thumbDir, "thumb_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(thumbFile).use { fos ->
                            firstFrame.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                        }
                        thumbnailPath = thumbFile.absolutePath
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "MediaMetadataRetriever failed on $permanentPath: ${e.message}")
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } else if (detectedCategory == "IMAGE") {
            actualDurationMs = 5000L // Default duration for image slide on video timeline
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(permanentPath, options)
                if (options.outWidth > 0 && options.outHeight > 0) {
                    videoWidth = options.outWidth
                    videoHeight = options.outHeight
                }
                thumbnailPath = permanentPath
            } catch (e: Exception) {
                Log.e(TAG, "BitmapFactory error reading image: ${e.message}")
            }
        }

        val totalSec = actualDurationMs / 1000
        val mins = totalSec / 60
        val secs = totalSec % 60
        val durationText = String.format("%02d:%02d", mins, secs)

        val resolutionOrType = when (detectedCategory) {
            "VIDEO" -> "${videoWidth}x${videoHeight} MP4"
            "AUDIO" -> "Stereo Audio (${actualDurationMs / 1000}s)"
            "IMAGE" -> "${videoWidth}x${videoHeight} Image"
            "LUT" -> "3D LUT Filter"
            else -> "Media Asset"
        }

        ImportedMediaResult(
            permanentFilePath = permanentPath,
            fileName = originalFileName,
            category = detectedCategory,
            durationMs = actualDurationMs,
            durationText = durationText,
            width = videoWidth,
            height = videoHeight,
            resolutionOrType = resolutionOrType,
            thumbnailPath = thumbnailPath
        )
    }

    /**
     * Extracts and caches a video frame bitmap at exact millisecond timestamp.
     */
    suspend fun extractVideoFrame(
        filePath: String,
        timeMs: Long
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (filePath.isBlank()) return@withContext null
        val file = File(filePath.removePrefix("file://"))
        if (!file.exists()) return@withContext null

        val cacheKey = "${file.absolutePath}:$timeMs"
        frameCache.get(cacheKey)?.let { return@withContext it }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val timeUs = timeMs * 1000L
            val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: retriever.getFrameAtTime()

            if (frame != null) {
                frameCache.put(cacheKey, frame)
                return@withContext frame
            }
        } catch (e: Exception) {
            Log.w(TAG, "extractVideoFrame for ${file.name} at $timeMs ms: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
        return@withContext null
    }

    data class PlaceholderVideoSpec(
        val id: String,
        val title: String,
        val description: String,
        val fileName: String,
        val durationSeconds: Int = 6,
        val startColor: Int,
        val endColor: Int,
        val tag: String
    )

    val SAMPLE_VIDEOS = listOf(
        PlaceholderVideoSpec(
            id = "sample_cinema",
            title = "FlowMonkey Cinema 4K",
            description = "Video sinematik gradien Indigo & Navy dengan animasi pulse",
            fileName = "sample_flowmonkey_cinema.mp4",
            durationSeconds = 6,
            startColor = Color.rgb(15, 23, 42),
            endColor = Color.rgb(99, 102, 241),
            tag = "Cinematic"
        ),
        PlaceholderVideoSpec(
            id = "sample_sunset",
            title = "Sunset Golden Hour",
            description = "Gradien Amber & Ruby Crimson hangat dengan dynamic counter",
            fileName = "sample_sunset_golden.mp4",
            durationSeconds = 6,
            startColor = Color.rgb(124, 45, 18),
            endColor = Color.rgb(245, 158, 11),
            tag = "Golden Hour"
        ),
        PlaceholderVideoSpec(
            id = "sample_cyberpunk",
            title = "Cyberpunk Neon Tokyo",
            description = "Nuansa Neon Violet & Electric Cyan dengan radar ring",
            fileName = "sample_cyberpunk_neon.mp4",
            durationSeconds = 6,
            startColor = Color.rgb(76, 29, 149),
            endColor = Color.rgb(6, 182, 212),
            tag = "Cyberpunk"
        ),
        PlaceholderVideoSpec(
            id = "sample_nature",
            title = "Emerald Forest Drone",
            description = "Pemandangan Forest Green & Mint cerah untuk footage alam",
            fileName = "sample_emerald_nature.mp4",
            durationSeconds = 6,
            startColor = Color.rgb(6, 78, 59),
            endColor = Color.rgb(16, 185, 129),
            tag = "Nature"
        ),
        PlaceholderVideoSpec(
            id = "sample_noir",
            title = "Monochrome City Noir",
            description = "Gaya film B&W monokrom klasik kontras tinggi",
            fileName = "sample_city_noir.mp4",
            durationSeconds = 6,
            startColor = Color.rgb(24, 24, 27),
            endColor = Color.rgb(113, 113, 122),
            tag = "B&W Noir"
        )
    )

    suspend fun getOrGenerateSampleVideo(
        context: Context,
        spec: PlaceholderVideoSpec
    ): String = createDefaultSampleVideoIfMissing(
        context = context,
        fileName = spec.fileName,
        titleText = spec.title,
        gradientStartColor = spec.startColor,
        gradientEndColor = spec.endColor,
        durationSeconds = spec.durationSeconds
    )

    /**
     * Generates a real 1080p MP4 sample video on device if missing, so the timeline always has real video files to play.
     */
    suspend fun createDefaultSampleVideoIfMissing(
        context: Context,
        fileName: String = "sample_video_clip_1.mp4",
        titleText: String = "FlowMonkey Cinema",
        gradientStartColor: Int = Color.rgb(24, 24, 38),
        gradientEndColor: Int = Color.rgb(99, 102, 241),
        durationSeconds: Int = 6
    ): String = withContext(Dispatchers.IO) {
        val mediaDir = File(context.filesDir, "imported_media").apply { mkdirs() }
        val sampleFile = File(mediaDir, fileName)
        if (sampleFile.exists() && sampleFile.length() > 5000) {
            return@withContext sampleFile.absolutePath
        }

        val width = 1280
        val height = 720
        val fps = 30
        val durationSeconds = 6
        val totalFrames = fps * durationSeconds
        val bitRate = 2_000_000

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var videoTrackIndex = -1
        var samplesWritten = 0

        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = codec.createInputSurface()
            codec.start()

            muxer = MediaMuxer(sampleFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 54f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(8f, 2f, 2f, Color.argb(180, 0, 0, 0))
            }
            val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(200, 220, 255)
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }

            for (frame in 0 until totalFrames) {
                val canvas = inputSurface.lockCanvas(null)
                val progress = frame.toFloat() / totalFrames

                val bgPaint = Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, width.toFloat(), height.toFloat(),
                        gradientStartColor,
                        gradientEndColor,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // Animated glowing circle
                val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb((80 + 120 * Math.sin(progress * Math.PI * 4)).toInt().coerceIn(30, 220), 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                }
                val cx = width / 2f + (Math.sin(progress * Math.PI * 2) * 80).toFloat()
                val cy = height / 2f - 40f
                canvas.drawCircle(cx, cy, 90f + (progress * 20f), circlePaint)

                canvas.drawText(titleText, width / 2f, height / 2f + 60f, textPaint)
                val timeStr = String.format("00:%02d / 00:%02d • Full Frame HD", (frame / fps), durationSeconds)
                canvas.drawText(timeStr, width / 2f, height / 2f + 110f, subTextPaint)

                inputSurface.unlockCanvasAndPost(canvas)

                // Drain encoder
                while (true) {
                    val status = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            videoTrackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (status >= 0) {
                        val encodedBuffer = codec.getOutputBuffer(status)
                        if (encodedBuffer != null) {
                            // MPEG4Writer already gets CSD in addTrack(); ignore codec config flags
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0 && muxerStarted && videoTrackIndex >= 0) {
                                encodedBuffer.position(bufferInfo.offset)
                                encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(videoTrackIndex, encodedBuffer, bufferInfo)
                                samplesWritten++
                            }
                        }
                        codec.releaseOutputBuffer(status, false)
                    }
                }
            }

            codec.signalEndOfInputStream()

            // Drain remaining buffers
            var eos = false
            var drainTries = 0
            while (!eos && drainTries < 50) {
                drainTries++
                val status = codec.dequeueOutputBuffer(bufferInfo, 20000)
                if (status >= 0) {
                    val encodedBuffer = codec.getOutputBuffer(status)
                    if (encodedBuffer != null) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0 && muxerStarted && videoTrackIndex >= 0) {
                            encodedBuffer.position(bufferInfo.offset)
                            encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedBuffer, bufferInfo)
                            samplesWritten++
                        }
                    }
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eos = true
                    }
                    codec.releaseOutputBuffer(status, false)
                } else if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    eos = true
                }
            }

            Log.d(TAG, "Successfully generated sample video with $samplesWritten samples at: ${sampleFile.absolutePath}")
            return@withContext sampleFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating default sample video: ${e.message}", e)
            return@withContext sampleFile.absolutePath
        } finally {
            try {
                codec?.stop()
            } catch (e: Exception) {}
            try {
                codec?.release()
            } catch (e: Exception) {}
            try {
                if (muxerStarted && samplesWritten > 0) {
                    muxer?.stop()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception stopping muxer: ${e.message}")
            }
            try {
                muxer?.release()
            } catch (e: Exception) {}
        }
    }

    /**
     * Clears cached frame memory if needed.
     */
    fun clearCache() {
        frameCache.evictAll()
    }
}
