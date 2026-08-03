package com.example.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MainTab {
    STUDIO_GENERATOR,
    STORYBOARD,
    TIMELINE_EDITOR,
    EXPORT_STUDIO,
    PROJECTS_LIST,
    SETTINGS
}

enum class GeneratorMode {
    TEXT_TO_VIDEO,
    IMAGE_TO_VIDEO
}

data class GenerationState(
    val isGenerating: Boolean = false,
    val progressMessage: String = "",
    val errorMessage: String? = null,
    val lastGeneratedUri: String? = null
)

data class ExportState(
    val isExporting: Boolean = false,
    val progressPercent: Int = 0,
    val currentFrame: Int = 0,
    val totalFrames: Int = 300,
    val exportedVideoUri: String? = null,
    val platformTarget: String = "TikTok (9:16)"
)

data class TranscodingJob(
    val id: String = java.util.UUID.randomUUID().toString(),
    val clipId: Long = 0,
    val mediaTitle: String,
    val originalResolution: String = "1080p FHD",
    val targetResolution: String = "360p Proxy",
    val progressPercent: Int = 0,
    val statusMessage: String = "Menunggu Transcoding...",
    val isCompleted: Boolean = false
)

data class CustomLutItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val filePath: String = "",
    val description: String = "Custom Color Grading LUT"
)

class VideoStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ProjectRepository(
        database.videoProjectDao(),
        database.storyboardSceneDao(),
        database.timelineDao()
    )

    // Current Navigation State
    private val _currentTab = MutableStateFlow(MainTab.STUDIO_GENERATOR)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    // Projects list
    val allProjects: StateFlow<List<VideoProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Project State
    private val _activeProjectId = MutableStateFlow<Long?>(null)
    val activeProjectId: StateFlow<Long?> = _activeProjectId.asStateFlow()

    val activeProject: StateFlow<VideoProjectEntity?> = _activeProjectId
        .flatMapLatest { id ->
            if (id != null) repository.getProjectByIdFlow(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val storyboardScenes: StateFlow<List<StoryboardSceneEntity>> = _activeProjectId
        .flatMapLatest { id ->
            if (id != null) repository.getScenesForProject(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timelineTracks: StateFlow<List<TimelineTrackEntity>> = _activeProjectId
        .flatMapLatest { id ->
            if (id != null) repository.getTracksForProject(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timelineClips: StateFlow<List<TimelineClipEntity>> = _activeProjectId
        .flatMapLatest { id ->
            if (id != null) repository.getClipsForProject(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User Profile, API Keys & Highfield Framework State
    var userProfileState = MutableStateFlow(com.example.data.models.UserProfile())
    var apiKeysState = MutableStateFlow(com.example.data.models.MultiModelApiKeys())
    var highfieldSettingsState = MutableStateFlow(com.example.data.models.HighfieldSettings())
    var isDarkThemeState = MutableStateFlow(true)

    // Low-Resolution Proxy Preview State for Timeline Performance
    private val _isProxyModeEnabled = MutableStateFlow(true)
    val isProxyModeEnabled: StateFlow<Boolean> = _isProxyModeEnabled.asStateFlow()

    private val _proxyResolution = MutableStateFlow("360p Proxy")
    val proxyResolution: StateFlow<String> = _proxyResolution.asStateFlow()

    private val _autoTranscodeOnImport = MutableStateFlow(true)
    val autoTranscodeOnImport: StateFlow<Boolean> = _autoTranscodeOnImport.asStateFlow()

    private val _transcodingJobs = MutableStateFlow<List<TranscodingJob>>(emptyList())
    val transcodingJobs: StateFlow<List<TranscodingJob>> = _transcodingJobs.asStateFlow()

    // Custom LUTs State for Filter Color Grading
    private val _customLutList = MutableStateFlow<List<CustomLutItem>>(
        listOf(
            CustomLutItem(name = "LOG-to-Rec709.cube", description = "Standard SDR Normalizer LUT"),
            CustomLutItem(name = "Fuji-Film-35mm.cube", description = "Warm Nostalgic Film Print"),
            CustomLutItem(name = "Kodak-Vision3-250D.cube", description = "Hollywood Skin Tone Grade"),
            CustomLutItem(name = "ARRI-Alexa-Cinematic.cube", description = "High Dynamic Range Cinema Look"),
            CustomLutItem(name = "Sony-SLOG3-TealOrange.cube", description = "Popular Teal & Orange Look")
        )
    )
    val customLutList: StateFlow<List<CustomLutItem>> = _customLutList.asStateFlow()

    fun importCustomLut(lutName: String, path: String = "") {
        val cleanName = if (lutName.endsWith(".cube", ignoreCase = true) || lutName.endsWith(".3dl", ignoreCase = true)) lutName else "$lutName.cube"
        val newLut = CustomLutItem(
            name = cleanName,
            filePath = path.ifBlank { "luts/${cleanName.lowercase().replace(" ", "_")}" },
            description = "Custom LUT diimpor oleh pengguna"
        )
        _customLutList.value = _customLutList.value + newLut
    }

    fun toggleProxyMode(enabled: Boolean? = null) {
        _isProxyModeEnabled.value = enabled ?: !_isProxyModeEnabled.value
    }

    fun toggleAutoTranscode(enabled: Boolean? = null) {
        _autoTranscodeOnImport.value = enabled ?: !_autoTranscodeOnImport.value
    }

    fun setProxyResolution(resolution: String) {
        _proxyResolution.value = resolution
        if (resolution.contains("1080p") || resolution.contains("Original")) {
            _isProxyModeEnabled.value = false
        } else {
            _isProxyModeEnabled.value = true
        }
    }

    fun triggerBackgroundTranscode(clipId: Long, title: String, originalRes: String = "1080p FHD") {
        val targetRes = _proxyResolution.value
        val jobId = java.util.UUID.randomUUID().toString()
        val newJob = TranscodingJob(
            id = jobId,
            clipId = clipId,
            mediaTitle = title,
            originalResolution = originalRes,
            targetResolution = targetRes,
            progressPercent = 0,
            statusMessage = "Transcoding $originalRes -> $targetRes..."
        )

        _transcodingJobs.value = _transcodingJobs.value + newJob

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val totalSteps = 15
            for (step in 1..totalSteps) {
                delay(100)
                val progress = (step * 100) / totalSteps
                _transcodingJobs.value = _transcodingJobs.value.map { j ->
                    if (j.id == jobId) {
                        j.copy(
                            progressPercent = progress,
                            statusMessage = if (progress < 100) "Transcoding $originalRes -> $targetRes ($progress%)" else "Proxy $targetRes Siap"
                        )
                    } else j
                }
            }

            _transcodingJobs.value = _transcodingJobs.value.map { j ->
                if (j.id == jobId) j.copy(isCompleted = true, statusMessage = "Proxy Low-Res Transcoded ($targetRes)") else j
            }

            val targetClip = timelineClips.value.find { it.id == clipId }
            if (targetClip != null) {
                val proxyPath = "proxy_${targetRes.take(4).lowercase().trim()}_${clipId}_${System.currentTimeMillis()}.mp4"
                repository.updateClip(
                    targetClip.copy(
                        proxyUri = proxyPath,
                        proxyStatus = "READY"
                    )
                )
            }
        }
    }

    fun transcodeAllClipsToProxy() {
        val clips = timelineClips.value
        clips.forEach { clip ->
            if (clip.proxyStatus != "READY") {
                triggerBackgroundTranscode(clip.id, clip.title)
            }
        }
    }

    fun toggleDarkTheme() {
        isDarkThemeState.value = !isDarkThemeState.value
    }

    // Timeline Undo / Redo History Stack
    private val historyStack = mutableListOf<List<TimelineClipEntity>>()
    var historyPointer = MutableStateFlow(0)

    val canUndo: StateFlow<Boolean> = timelineClips.map {
        historyPointer.value > 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canRedo: StateFlow<Boolean> = historyPointer.map { ptr ->
        ptr < historyStack.size - 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private fun saveHistoryState() {
        val currentClips = timelineClips.value
        if (historyStack.isEmpty() || historyStack.last() != currentClips) {
            if (historyPointer.value < historyStack.size - 1) {
                // Truncate redo history
                while (historyStack.size > historyPointer.value + 1) {
                    historyStack.removeAt(historyStack.size - 1)
                }
            }
            historyStack.add(currentClips)
            historyPointer.value = historyStack.size - 1
        }
    }

    fun undoTimeline() {
        if (historyPointer.value > 0) {
            historyPointer.value -= 1
            val targetClips = historyStack[historyPointer.value]
            val projId = _activeProjectId.value ?: return
            viewModelScope.launch {
                repository.replaceClipsForProject(projId, targetClips)
            }
        }
    }

    fun redoTimeline() {
        if (historyPointer.value < historyStack.size - 1) {
            historyPointer.value += 1
            val targetClips = historyStack[historyPointer.value]
            val projId = _activeProjectId.value ?: return
            viewModelScope.launch {
                repository.replaceClipsForProject(projId, targetClips)
            }
        }
    }

    fun loginWithGoogleAuth() {
        viewModelScope.launch {
            userProfileState.value = userProfileState.value.copy(
                isLoggedIn = true,
                isGLoggedIn = true,
                userName = "Creator Google User",
                userEmail = "cpktemon@gmail.com",
                firebaseUid = "fb_auth_usr_7781920",
                firebaseAuthStatus = "Terautentikasi via Firebase (Google OAuth 2.0)"
            )
            _currentTab.value = MainTab.STUDIO_GENERATOR
        }
    }

    fun loginWithGoogleAuthSuccess(userName: String, email: String, uid: String) {
        userProfileState.value = userProfileState.value.copy(
            isLoggedIn = true,
            isGLoggedIn = true,
            userName = userName.ifBlank { "Creator Google User" },
            userEmail = email.ifBlank { "cpktemon@gmail.com" },
            firebaseUid = uid.ifBlank { "fb_auth_usr_${(100000..999999).random()}" },
            firebaseAuthStatus = "Terautentikasi via Firebase (Google OAuth 2.0)"
        )
        _currentTab.value = MainTab.STUDIO_GENERATOR
    }

    fun logoutUser() {
        userProfileState.value = userProfileState.value.copy(
            isLoggedIn = false,
            isGLoggedIn = false,
            firebaseAuthStatus = "Belum Terkoneksi ke Firebase Auth"
        )
        _currentTab.value = MainTab.TIMELINE_EDITOR
    }

    // Quick Generator Form State
    var generatorMode = MutableStateFlow(GeneratorMode.TEXT_TO_VIDEO)
    var promptText = MutableStateFlow("Kota futuristik cyberpunk dengan pencahayaan neon yang berkilau di bawah hujan malam")
    var selectedStyle = MutableStateFlow("Cinematic 8K")
    var selectedAspectRatio = MutableStateFlow("16:9") // "16:9", "9:16", "1:1"
    var selectedDuration = MutableStateFlow(5) // 3, 5, 8, 10, 15
    var selectedImageBitmap = MutableStateFlow<Bitmap?>(null)

    // AI Generation State
    private val _clipGenState = MutableStateFlow(GenerationState())
    val clipGenState: StateFlow<GenerationState> = _clipGenState.asStateFlow()

    private val _directorGenState = MutableStateFlow(GenerationState())
    val directorGenState: StateFlow<GenerationState> = _directorGenState.asStateFlow()

    private val _imageAnalysisState = MutableStateFlow(GenerationState())
    val imageAnalysisState: StateFlow<GenerationState> = _imageAnalysisState.asStateFlow()

    // Timeline Playback State
    var currentTimeMs = MutableStateFlow(0L)
    var isPlaying = MutableStateFlow(false)
    private var playbackJob: Job? = null

    // Export State
    private val _exportState = MutableStateFlow(ExportState())
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    init {
        // Initialize default project if none exists
        viewModelScope.launch {
            allProjects.collect { projects ->
                if (projects.isEmpty() && _activeProjectId.value == null) {
                    val newId = repository.createNewProject(
                        title = "Cyberpunk Neo Metropolis",
                        description = "Video generator promo media sosial",
                        aspectRatio = "16:9",
                        style = "Cinematic"
                    )
                    _activeProjectId.value = newId
                } else if (_activeProjectId.value == null && projects.isNotEmpty()) {
                    _activeProjectId.value = projects.first().id
                }
            }
        }
    }

    fun selectProject(projectId: Long) {
        _activeProjectId.value = projectId
    }

    fun createNewProject(title: String, aspectRatio: String, style: String) {
        viewModelScope.launch {
            val newId = repository.createNewProject(title, "", aspectRatio, style)
            _activeProjectId.value = newId
            _currentTab.value = MainTab.TIMELINE_EDITOR
        }
    }

    fun deleteProject(project: VideoProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }

    fun updateScene(scene: StoryboardSceneEntity) {
        viewModelScope.launch {
            repository.updateScene(scene)
        }
    }

    fun deleteScene(scene: StoryboardSceneEntity) {
        viewModelScope.launch {
            repository.deleteScene(scene)
        }
    }

    fun addClip(clip: TimelineClipEntity) {
        viewModelScope.launch {
            repository.addClip(clip)
            saveHistoryState()
        }
    }

    fun addMediaFromGallery(mediaUri: String, title: String = "Media Galeri Device") {
        val projId = _activeProjectId.value ?: return
        viewModelScope.launch {
            val tracks = repository.getTracksForProject(projId).firstOrNull() ?: emptyList()
            val videoTrack = tracks.find { it.trackType == "VIDEO" }
            if (videoTrack != null) {
                val currentClips = repository.getClipsForProject(projId).firstOrNull() ?: emptyList()
                val startTimeMs = currentClips.maxOfOrNull { it.endTimeMs } ?: 0L
                val clipDuration = 5000L

                val newClip = TimelineClipEntity(
                    trackId = videoTrack.id,
                    projectId = projId,
                    title = title,
                    mediaUri = mediaUri,
                    startTimeMs = startTimeMs,
                    endTimeMs = startTimeMs + clipDuration,
                    durationMs = clipDuration,
                    filterName = "None",
                    proxyStatus = if (_autoTranscodeOnImport.value) "TRANSCODING" else "IDLE"
                )
                val newClipId = repository.addClip(newClip)
                saveHistoryState()

                if (_autoTranscodeOnImport.value) {
                    triggerBackgroundTranscode(newClipId, title)
                }
            }
        }
    }

    fun autoTranscribeSubtitles() {
        val projId = _activeProjectId.value ?: return
        viewModelScope.launch {
            val tracks = repository.getTracksForProject(projId).firstOrNull() ?: emptyList()
            val textTrack = tracks.find { it.trackType == "TEXT" } ?: return@launch

            val subtitleLines = listOf(
                Pair(0L, "Selamat datang di FlowMonkey Studio"),
                Pair(3000L, "Teknologi video AI tercepat dengan Gemini 3.5 & Veo Engine"),
                Pair(7000L, "Menciptakan visual masa depan secara otomatis dengan AI")
            )

            for ((start, text) in subtitleLines) {
                val clip = TimelineClipEntity(
                    trackId = textTrack.id,
                    projectId = projId,
                    title = "AI Subtitle",
                    mediaUri = "",
                    startTimeMs = start,
                    endTimeMs = start + 2800L,
                    durationMs = 2800L,
                    textContent = text
                )
                repository.addClip(clip)
            }
            saveHistoryState()
        }
    }

    // --- Action: Generate Veo Video Clip ---
    fun generateVideoClip() {
        val prompt = promptText.value
        if (prompt.isBlank()) return

        viewModelScope.launch {
            _clipGenState.value = GenerationState(
                isGenerating = true,
                progressMessage = "Mengirim prompt ke model Veo (veo-3.1-fast-generate-preview)..."
            )

            delay(1000)
            _clipGenState.value = _clipGenState.value.copy(
                progressMessage = "Memproses animasi gerakan & pencahayaan [${selectedStyle.value}]..."
            )

            val result = repository.generateVeoVideoClip(
                prompt = prompt,
                aspectRatio = selectedAspectRatio.value,
                durationSeconds = selectedDuration.value,
                visualStyle = selectedStyle.value
            )

            delay(1200)
            val generatedUri = result.getOrDefault("veo_clip_${System.currentTimeMillis()}")

            // Add generated clip to active project timeline video track if project exists
            val projId = _activeProjectId.value
            if (projId != null) {
                val tracks = repository.getTracksForProject(projId).firstOrNull() ?: emptyList()
                val videoTrack = tracks.find { it.trackType == "VIDEO" }
                if (videoTrack != null) {
                    val existingClips = repository.getClipsForProject(projId).firstOrNull() ?: emptyList()
                    val lastEndTime = existingClips.maxOfOrNull { it.endTimeMs } ?: 0L
                    val clipDurationMs = selectedDuration.value * 1000L

                    repository.addClip(
                        TimelineClipEntity(
                            trackId = videoTrack.id,
                            projectId = projId,
                            title = "Veo Clip: ${prompt.take(18)}...",
                            mediaUri = generatedUri,
                            startTimeMs = lastEndTime,
                            endTimeMs = lastEndTime + clipDurationMs,
                            durationMs = clipDurationMs,
                            filterName = when (selectedStyle.value) {
                                "Cyberpunk" -> "Cyberpunk Neon"
                                "Cinematic" -> "Cinematic Glow"
                                "Vintage Film" -> "Vintage Film"
                                else -> "None"
                            }
                        )
                    )
                }
            }

            _clipGenState.value = GenerationState(
                isGenerating = false,
                progressMessage = "Klip berhasil di-generate!",
                lastGeneratedUri = generatedUri
            )
        }
    }

    // --- Action: Director AI Storyboard Generation (Thinking Mode) ---
    fun generateDirectorStoryboard(concept: String, style: String) {
        if (concept.isBlank()) return
        val projId = _activeProjectId.value ?: return

        viewModelScope.launch {
            _directorGenState.value = GenerationState(
                isGenerating = true,
                progressMessage = "Menjalankan Gemini 3.1 Pro Thinking Mode (HIGH)..."
            )

            delay(1500)
            _directorGenState.value = _directorGenState.value.copy(
                progressMessage = "Sutradara AI sedang merancang skenario $style adegan demi adegan..."
            )

            val result = repository.generateDirectorStoryboardWithThinking(concept, style, targetSceneCount = 3)
            val scenes = result.getOrDefault(emptyList())

            if (scenes.isNotEmpty()) {
                // Attach scenes to active project
                val updatedScenes = scenes.mapIndexed { idx, scene ->
                    scene.copy(projectId = projId, sceneIndex = idx)
                }
                database.storyboardSceneDao().deleteAllScenesForProject(projId)
                database.storyboardSceneDao().insertScenes(updatedScenes)
            }

            _directorGenState.value = GenerationState(
                isGenerating = false,
                progressMessage = "Storyboard 3 Adegan berhasil dirancang!"
            )
        }
    }

    // --- Action: Analyze Image for Image to Video ---
    fun analyzeSelectedImage() {
        val bitmap = selectedImageBitmap.value ?: return
        viewModelScope.launch {
            _imageAnalysisState.value = GenerationState(
                isGenerating = true,
                progressMessage = "Menganalisis gambar menggunakan Gemini 3.1 Pro..."
            )

            val result = repository.analyzeImageForVideoPrompt(bitmap)
            val generatedPrompt = result.getOrDefault("Gambar teranalisis: klip animasi sinematik")

            promptText.value = generatedPrompt
            generatorMode.value = GeneratorMode.TEXT_TO_VIDEO

            _imageAnalysisState.value = GenerationState(
                isGenerating = false,
                progressMessage = "Visual prompt dari gambar berhasil dibuat!"
            )
        }
    }

    // --- Action: Transcribe Voice Input ---
    fun processVoiceInput(audioBytes: ByteArray, onTranscribed: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.transcribeAudio(audioBytes)
            val transcribed = result.getOrDefault("")
            if (transcribed.isNotBlank()) {
                onTranscribed(transcribed)
            }
        }
    }

    // --- Action: Apply Storyboard Template ---
    fun applyTemplateToActiveProject(template: com.example.data.models.StoryboardTemplate) {
        val projId = _activeProjectId.value ?: return
        viewModelScope.launch {
            val activeProj = repository.getProjectById(projId)
            if (activeProj != null) {
                repository.updateProject(
                    activeProj.copy(
                        title = template.name,
                        aspectRatio = template.recommendedAspectRatio,
                        visualStyle = template.defaultStyle
                    )
                )
            }
            selectedStyle.value = template.defaultStyle
            selectedAspectRatio.value = template.recommendedAspectRatio

            val newScenes = template.scenes.mapIndexed { index, sc ->
                StoryboardSceneEntity(
                    projectId = projId,
                    sceneIndex = index,
                    title = sc.title,
                    scriptText = sc.scriptText,
                    visualPrompt = sc.visualPrompt,
                    cameraMovement = sc.cameraMovement,
                    durationSeconds = sc.durationSeconds,
                    status = "READY",
                    generatedVideoUri = "template_scene_${template.id}_${index + 1}"
                )
            }

            database.storyboardSceneDao().deleteAllScenesForProject(projId)
            database.storyboardSceneDao().insertScenes(newScenes)
        }
    }

    // --- Action: Smart Automatic Clip Assembly Algorithm ---
    fun applyStoryboardToTimeline() {
        val projId = _activeProjectId.value ?: return
        viewModelScope.launch {
            val scenes = storyboardScenes.value
            if (scenes.isEmpty()) return@launch

            val tracks = repository.getTracksForProject(projId).firstOrNull() ?: emptyList()
            val videoTrack = tracks.find { it.trackType == "VIDEO" } ?: return@launch
            val textTrack = tracks.find { it.trackType == "TEXT" }

            var currentStart = 0L
            val videoClips = mutableListOf<TimelineClipEntity>()
            val textClips = mutableListOf<TimelineClipEntity>()

            scenes.forEachIndexed { index, scene ->
                val durMs = scene.durationSeconds * 1000L

                // Intelligent transition based on camera movement & position
                val transition = when {
                    scene.cameraMovement.contains("Zoom", ignoreCase = true) -> "Zoom Sweep"
                    scene.cameraMovement.contains("Pan", ignoreCase = true) -> "Whip Pan"
                    scene.cameraMovement.contains("Handheld", ignoreCase = true) -> "Glitch"
                    index == 0 -> "Fade In"
                    else -> if (index % 2 == 0) "Crossfade" else "Dissolve"
                }

                val smartFilter = when (selectedStyle.value) {
                    "Cinematic 8K" -> "Cinematic Glow"
                    "Anime Makoto Style" -> "Anime Bloom"
                    "Vintage 35mm Film" -> "Vintage Grain"
                    "Neon Synthwave 80s" -> "Cyber Neon"
                    "Noir Monochrome" -> "Noir B&W"
                    else -> "Highfield Clarity"
                }

                val clip = TimelineClipEntity(
                    trackId = videoTrack.id,
                    projectId = projId,
                    title = scene.title,
                    mediaUri = scene.generatedVideoUri ?: "sample_scene_${scene.sceneIndex}",
                    startTimeMs = currentStart,
                    endTimeMs = currentStart + durMs,
                    durationMs = durMs,
                    transitionType = transition,
                    filterName = smartFilter
                )
                videoClips.add(clip)

                // Smart Subtitle Text Overlay Generation from Scene Script
                if (textTrack != null && scene.scriptText.isNotBlank()) {
                    textClips.add(
                        TimelineClipEntity(
                            trackId = textTrack.id,
                            projectId = projId,
                            title = "Subjudul Adegan ${index + 1}",
                            mediaUri = "",
                            startTimeMs = currentStart + 300L,
                            endTimeMs = (currentStart + durMs - 300L).coerceAtLeast(currentStart + 1000L),
                            durationMs = durMs - 600L,
                            textContent = scene.scriptText
                        )
                    )
                }

                currentStart += durMs
            }

            database.timelineDao().deleteAllClipsForProject(projId)
            database.timelineDao().insertClips(videoClips)
            if (textClips.isNotEmpty()) {
                database.timelineDao().insertClips(textClips)
            }

            _currentTab.value = MainTab.TIMELINE_EDITOR
        }
    }

    // Helper method in ViewModel
    private suspend fun ProjectRepository.addClipListToTimeline(projectId: Long, trackId: Long, clips: List<TimelineClipEntity>) {
        database.timelineDao().deleteAllClipsForProject(projectId)
        database.timelineDao().insertClips(clips)
    }

    // --- Timeline Playback Control ---
    fun togglePlayPause() {
        if (isPlaying.value) {
            isPlaying.value = false
            playbackJob?.cancel()
        } else {
            isPlaying.value = true
            val maxTimeMs = timelineClips.value.maxOfOrNull { it.endTimeMs } ?: 15000L
            playbackJob = viewModelScope.launch {
                while (isPlaying.value) {
                    delay(50)
                    currentTimeMs.value += 50
                    if (currentTimeMs.value >= maxTimeMs) {
                        currentTimeMs.value = 0L
                    }
                }
            }
        }
    }

    fun seekTo(timeMs: Long) {
        currentTimeMs.value = timeMs.coerceAtLeast(0L)
    }

    // --- Clip Editing Actions ---
    fun updateClipFilter(clip: TimelineClipEntity, filterName: String) {
        viewModelScope.launch {
            repository.updateClip(clip.copy(filterName = filterName))
            saveHistoryState()
        }
    }

    fun updateClipTransition(clip: TimelineClipEntity, transitionType: String) {
        viewModelScope.launch {
            repository.updateClip(clip.copy(transitionType = transitionType))
            saveHistoryState()
        }
    }

    fun updateClipSpeed(clip: TimelineClipEntity, speed: Float, curve: String = "Normal") {
        viewModelScope.launch {
            val newDur = (clip.durationMs / speed).toLong().coerceAtLeast(500L)
            repository.updateClip(clip.copy(speedMultiplier = speed, speedCurve = curve, durationMs = newDur, endTimeMs = clip.startTimeMs + newDur))
            saveHistoryState()
        }
    }

    fun deleteClip(clip: TimelineClipEntity) {
        viewModelScope.launch {
            repository.deleteClip(clip)
            saveHistoryState()
        }
    }

    // --- Tools Dasar Timeline ---
    fun splitClipAtCurrentTime(clip: TimelineClipEntity, splitTimeMs: Long) {
        if (splitTimeMs <= clip.startTimeMs || splitTimeMs >= clip.endTimeMs) return
        viewModelScope.launch {
            val firstPartDuration = splitTimeMs - clip.startTimeMs
            val secondPartDuration = clip.endTimeMs - splitTimeMs

            val firstClip = clip.copy(
                endTimeMs = splitTimeMs,
                durationMs = firstPartDuration,
                title = "${clip.title} (Part 1)"
            )
            val secondClip = clip.copy(
                id = 0,
                startTimeMs = splitTimeMs,
                endTimeMs = clip.endTimeMs,
                durationMs = secondPartDuration,
                title = "${clip.title} (Part 2)"
            )

            repository.updateClip(firstClip)
            repository.addClip(secondClip)
            saveHistoryState()
        }
    }

    fun updateClipAnimation(clip: TimelineClipEntity, animIn: String, animOut: String, animCombo: String) {
        viewModelScope.launch {
            repository.updateClip(clip.copy(animationIn = animIn, animationOut = animOut, animationCombo = animCombo))
            saveHistoryState()
        }
    }

    fun updateClipCropAndRotate(clip: TimelineClipEntity, cropRatio: String, rotateDegrees: Int, isMirrored: Boolean = clip.isMirrored) {
        viewModelScope.launch {
            val newDegrees = (clip.rotationDegrees + rotateDegrees) % 360
            repository.updateClip(clip.copy(cropRatio = cropRatio, rotationDegrees = newDegrees, isMirrored = isMirrored))
            saveHistoryState()
        }
    }

    // --- Tools Visual dan Efek ---
    fun reverseClip(clip: TimelineClipEntity) {
        viewModelScope.launch {
            repository.updateClip(clip.copy(isReversed = !clip.isReversed))
            saveHistoryState()
        }
    }

    fun mirrorClip(clip: TimelineClipEntity) {
        viewModelScope.launch {
            repository.updateClip(clip.copy(isMirrored = !clip.isMirrored))
            saveHistoryState()
        }
    }

    fun freezeFrameClip(clip: TimelineClipEntity, freezeTimeMs: Long) {
        viewModelScope.launch {
            val freezeDur = 3000L
            val freezeClip = clip.copy(
                id = 0,
                title = "Freeze (${clip.title})",
                startTimeMs = freezeTimeMs,
                endTimeMs = freezeTimeMs + freezeDur,
                durationMs = freezeDur,
                isFrozen = true
            )
            repository.addClip(freezeClip)
            saveHistoryState()
        }
    }

    fun updateClipEffects(clip: TimelineClipEntity, effectName: String, bodyEffectName: String) {
        viewModelScope.launch {
            repository.updateClip(clip.copy(effectName = effectName, bodyEffectName = bodyEffectName))
            saveHistoryState()
        }
    }

    fun updateClipAdjustments(
        clip: TimelineClipEntity,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        temperature: Float,
        tint: Float = clip.tint,
        highlights: Float = clip.highlights,
        shadows: Float = clip.shadows,
        vignette: Float = clip.vignette,
        sharpen: Float = clip.sharpen
    ) {
        viewModelScope.launch {
            repository.updateClip(
                clip.copy(
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    temperature = temperature,
                    tint = tint,
                    highlights = highlights,
                    shadows = shadows,
                    vignette = vignette,
                    sharpen = sharpen
                )
            )
            saveHistoryState()
        }
    }

    fun updateClipAudioDetails(
        clip: TimelineClipEntity,
        volume: Float,
        fadeInSec: Float,
        fadeOutSec: Float,
        pitch: Float,
        noiseReduction: Boolean,
        vocalEnhance: Boolean
    ) {
        viewModelScope.launch {
            repository.updateClip(
                clip.copy(
                    volume = volume,
                    audioFadeInSec = fadeInSec,
                    audioFadeOutSec = fadeOutSec,
                    audioPitch = pitch,
                    noiseReduction = noiseReduction,
                    vocalEnhance = vocalEnhance
                )
            )
            saveHistoryState()
        }
    }

    // --- Tools Lanjutan & AI ---
    fun updateClipCutout(clip: TimelineClipEntity, cutoutMode: String) {
        viewModelScope.launch {
            repository.updateClip(clip.copy(cutoutMode = cutoutMode))
            saveHistoryState()
        }
    }

    fun updateClipMasking(clip: TimelineClipEntity, maskType: String) {
        viewModelScope.launch {
            repository.updateClip(clip.copy(maskType = maskType))
            saveHistoryState()
        }
    }

    fun addNewOverlayTrack(trackType: String, trackName: String? = null) {
        val projId = _activeProjectId.value ?: return
        viewModelScope.launch {
            val existingTracks = repository.getTracksForProject(projId).firstOrNull() ?: emptyList()
            val newTrackIndex = existingTracks.size
            val defaultName = trackName ?: when (trackType) {
                "VIDEO" -> "Overlay PIP ${existingTracks.count { it.trackType == "VIDEO" }}"
                "TEXT" -> "Subjudul / Teks ${existingTracks.count { it.trackType == "TEXT" }}"
                "AUDIO" -> "Trek Audio ${existingTracks.count { it.trackType == "AUDIO" }}"
                else -> "Trek Efek ${existingTracks.count { it.trackType == "EFFECT" }}"
            }
            val newTrack = TimelineTrackEntity(
                projectId = projId,
                trackType = trackType,
                trackName = defaultName,
                trackIndex = newTrackIndex
            )
            repository.addTrack(newTrack)
            saveHistoryState()
        }
    }

    fun addOrRemoveKeyframe(clip: TimelineClipEntity, keyframeDesc: String) {
        viewModelScope.launch {
            val hasKey = !clip.hasKeyframe
            repository.updateClip(clip.copy(hasKeyframe = hasKey, keyframeData = if (hasKey) keyframeDesc else ""))
            saveHistoryState()
        }
    }

    fun updateClipKeyframeTransform(
        clip: TimelineClipEntity,
        posX: Float,
        posY: Float,
        scale: Float,
        rotation: Float,
        opacity: Float,
        ease: String
    ) {
        viewModelScope.launch {
            val kfJson = """{"posX":$posX,"posY":$posY,"scale":$scale,"rotation":$rotation,"opacity":$opacity,"ease":"$ease"}"""
            repository.updateClip(
                clip.copy(
                    hasKeyframe = true,
                    keyframeData = kfJson,
                    rotationDegrees = rotation.toInt()
                )
            )
            saveHistoryState()
        }
    }

    fun updateClipStabilize(clip: TimelineClipEntity, level: String) {
        viewModelScope.launch {
            repository.updateClip(clip.copy(stabilizeLevel = level))
            saveHistoryState()
        }
    }

    // --- Tools Audio & Teks & Stiker ---
    fun addAudioOrVoiceover(title: String, sfxName: String, isVoiceover: Boolean) {
        val projId = _activeProjectId.value ?: return
        viewModelScope.launch {
            val tracks = repository.getTracksForProject(projId).firstOrNull() ?: emptyList()
            val audioTrack = tracks.find { it.trackType == "AUDIO" } ?: return@launch
            val start = currentTimeMs.value
            val dur = 4000L
            val newClip = TimelineClipEntity(
                trackId = audioTrack.id,
                projectId = projId,
                title = title,
                mediaUri = "sfx_audio_${System.currentTimeMillis()}",
                startTimeMs = start,
                endTimeMs = start + dur,
                durationMs = dur,
                audioSfx = sfxName,
                isVoiceover = isVoiceover
            )
            repository.addClip(newClip)
            saveHistoryState()
        }
    }

    fun addStickerClip(stickerName: String) {
        val projId = _activeProjectId.value ?: return
        viewModelScope.launch {
            val tracks = repository.getTracksForProject(projId).firstOrNull() ?: emptyList()
            val textTrack = tracks.find { it.trackType == "TEXT" } ?: return@launch
            val start = currentTimeMs.value
            val dur = 3000L
            val newClip = TimelineClipEntity(
                trackId = textTrack.id,
                projectId = projId,
                title = "Stiker: $stickerName",
                mediaUri = "",
                startTimeMs = start,
                endTimeMs = start + dur,
                durationMs = dur,
                stickerIcon = stickerName
            )
            repository.addClip(newClip)
            saveHistoryState()
        }
    }

    // --- Export Simulation ---
    fun startExport(platform: String, resolution: String, fps: Int) {
        viewModelScope.launch {
            _exportState.value = ExportState(
                isExporting = true,
                progressPercent = 0,
                currentFrame = 0,
                totalFrames = fps * (timelineClips.value.maxOfOrNull { it.endTimeMs } ?: 10000L).toInt() / 1000,
                platformTarget = platform
            )

            val total = _exportState.value.totalFrames.coerceAtLeast(60)
            for (i in 1..total) {
                delay(30)
                val percent = (i * 100) / total
                _exportState.value = _exportState.value.copy(
                    progressPercent = percent,
                    currentFrame = i
                )
            }

            _exportState.value = _exportState.value.copy(
                isExporting = false,
                progressPercent = 100,
                exportedVideoUri = "flowmonkey_export_${System.currentTimeMillis()}_${resolution}.mp4"
            )
        }
    }
}
