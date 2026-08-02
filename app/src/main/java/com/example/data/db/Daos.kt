package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoProjectDao {
    @Query("SELECT * FROM video_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<VideoProjectEntity>>

    @Query("SELECT * FROM video_projects WHERE id = :id")
    fun getProjectByIdFlow(id: Long): Flow<VideoProjectEntity?>

    @Query("SELECT * FROM video_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): VideoProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProjectEntity): Long

    @Update
    suspend fun updateProject(project: VideoProjectEntity)

    @Delete
    suspend fun deleteProject(project: VideoProjectEntity)
}

@Dao
interface StoryboardSceneDao {
    @Query("SELECT * FROM storyboard_scenes WHERE projectId = :projectId ORDER BY sceneIndex ASC")
    fun getScenesForProject(projectId: Long): Flow<List<StoryboardSceneEntity>>

    @Query("SELECT * FROM storyboard_scenes WHERE projectId = :projectId ORDER BY sceneIndex ASC")
    suspend fun getScenesList(projectId: Long): List<StoryboardSceneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: StoryboardSceneEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<StoryboardSceneEntity>)

    @Update
    suspend fun updateScene(scene: StoryboardSceneEntity)

    @Delete
    suspend fun deleteScene(scene: StoryboardSceneEntity)

    @Query("DELETE FROM storyboard_scenes WHERE projectId = :projectId")
    suspend fun deleteAllScenesForProject(projectId: Long)
}

@Dao
interface TimelineDao {
    @Query("SELECT * FROM timeline_tracks WHERE projectId = :projectId ORDER BY trackIndex ASC")
    fun getTracksForProject(projectId: Long): Flow<List<TimelineTrackEntity>>

    @Query("SELECT * FROM timeline_tracks WHERE projectId = :projectId ORDER BY trackIndex ASC")
    suspend fun getTracksListForProject(projectId: Long): List<TimelineTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TimelineTrackEntity): Long

    @Query("SELECT * FROM timeline_clips WHERE projectId = :projectId ORDER BY startTimeMs ASC")
    fun getClipsForProject(projectId: Long): Flow<List<TimelineClipEntity>>

    @Query("SELECT * FROM timeline_clips WHERE trackId = :trackId ORDER BY startTimeMs ASC")
    fun getClipsForTrack(trackId: Long): Flow<List<TimelineClipEntity>>

    @Query("SELECT * FROM timeline_clips WHERE projectId = :projectId ORDER BY startTimeMs ASC")
    suspend fun getClipsListForProject(projectId: Long): List<TimelineClipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: TimelineClipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClips(clips: List<TimelineClipEntity>)

    @Update
    suspend fun updateClip(clip: TimelineClipEntity)

    @Delete
    suspend fun deleteClip(clip: TimelineClipEntity)

    @Query("DELETE FROM timeline_clips WHERE projectId = :projectId")
    suspend fun deleteAllClipsForProject(projectId: Long)
}
