package com.mediadock.app.core.ai

import com.mediadock.app.core.database.dao.DownloadDao
import com.mediadock.app.core.database.dao.HistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AiDuplicateReport(
    val isDuplicate: Boolean,
    val isAlreadyDownloading: Boolean = false,
    val existingTitle: String? = null,
    val existingLocalUri: String? = null,
    val message: String? = null
)

@Singleton
class AiStorageGuard @Inject constructor(
    private val historyDao: HistoryDao,
    private val downloadDao: DownloadDao
) {
    suspend fun checkDuplicate(url: String): AiDuplicateReport = withContext(Dispatchers.IO) {
        val canonical = AiLinkBrain.canonicalize(url)

        // Check active queue
        val activeList = downloadDao.getActiveDownloadsList()
        val activeMatch = activeList.firstOrNull {
            AiLinkBrain.canonicalize(it.sourceUrl) == canonical || it.sourceUrl == url
        }
        if (activeMatch != null && (activeMatch.status == "DOWNLOADING" || activeMatch.status == "QUEUED" || activeMatch.status == "PROCESSING")) {
            return@withContext AiDuplicateReport(
                isDuplicate = true,
                isAlreadyDownloading = true,
                existingTitle = activeMatch.mediaTitle,
                message = "This link is already in your active queue (${activeMatch.status})."
            )
        }

        // Check history
        val historyList = historyDao.getAllHistoryList()
        val historyMatch = historyList.firstOrNull {
            AiLinkBrain.canonicalize(it.sourceUrl) == canonical || it.sourceUrl == url
        }

        if (historyMatch != null) {
            return@withContext AiDuplicateReport(
                isDuplicate = true,
                isAlreadyDownloading = false,
                existingTitle = historyMatch.title,
                existingLocalUri = historyMatch.localUri,
                message = "You already downloaded \"${historyMatch.title}\"."
            )
        }

        AiDuplicateReport(isDuplicate = false)
    }
}
