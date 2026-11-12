package com.mediadock.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mediadock.app.core.database.dao.DownloadDao
import com.mediadock.app.core.database.dao.HistoryDao
import com.mediadock.app.core.database.entity.DownloadEntity
import com.mediadock.app.core.database.entity.HistoryEntity

@Database(
    entities = [
        DownloadEntity::class,
        HistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MediaDockDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun historyDao(): HistoryDao
}
