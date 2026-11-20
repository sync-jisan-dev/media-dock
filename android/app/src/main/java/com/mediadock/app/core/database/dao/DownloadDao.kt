package com.mediadock.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mediadock.app.core.database.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE jobId = :jobId")
    fun getDownload(jobId: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE jobId = :jobId")
    suspend fun getDownloadSync(jobId: String): DownloadEntity?

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'QUEUED', 'PROCESSING', 'DOWNLOADING', 'PAUSED', 'FAILED') ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'QUEUED', 'PROCESSING', 'DOWNLOADING', 'PAUSED', 'FAILED') ORDER BY createdAt DESC")
    suspend fun getActiveDownloadsList(): List<DownloadEntity>

    @Query("SELECT COUNT(*) FROM downloads WHERE status IN ('QUEUED', 'PROCESSING', 'DOWNLOADING')")
    fun getActiveDownloadsCount(): Flow<Int>

    @Query("UPDATE downloads SET progress = :progress, downloadSpeed = :speed, etaSeconds = :eta, downloadedBytes = :downloadedBytes WHERE jobId = :jobId")
    suspend fun updateProgress(jobId: String, progress: Float, speed: Long, eta: Long, downloadedBytes: Long)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error, completedAt = :completedAt, localUri = :localUri WHERE jobId = :jobId")
    suspend fun updateStatus(jobId: String, status: String, error: String? = null, completedAt: Long? = null, localUri: String? = null)

    @Query("UPDATE downloads SET remoteFileUrl = :fileUrl WHERE jobId = :jobId")
    suspend fun updateFileUrl(jobId: String, fileUrl: String)

    @Query("DELETE FROM downloads WHERE jobId = :jobId")
    suspend fun delete(jobId: String)
}
