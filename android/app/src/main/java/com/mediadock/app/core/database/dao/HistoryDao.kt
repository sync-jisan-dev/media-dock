package com.mediadock.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mediadock.app.core.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    @Query("SELECT * FROM download_history ORDER BY downloadedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM download_history ORDER BY downloadedAt DESC")
    suspend fun getAllHistoryList(): List<HistoryEntity>

    @Query("SELECT * FROM download_history WHERE title LIKE '%' || :query || '%' OR sourceUrl LIKE '%' || :query || '%' ORDER BY downloadedAt DESC")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM download_history WHERE mediaType = :mediaType ORDER BY downloadedAt DESC")
    fun getHistoryByType(mediaType: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM download_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HistoryEntity?

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM download_history")
    suspend fun clearAll()
}
