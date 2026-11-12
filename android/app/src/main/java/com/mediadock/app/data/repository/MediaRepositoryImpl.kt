package com.mediadock.app.data.repository

import com.mediadock.app.core.common.Resource
import com.mediadock.app.core.network.MediaDockApi
import com.mediadock.app.core.security.SecureTokenManager
import com.mediadock.app.data.remote.dto.InspectMediaRequestDto
import com.mediadock.app.domain.model.ExtractedImage
import com.mediadock.app.domain.model.MediaDetails
import com.mediadock.app.domain.model.SubtitleTrack
import com.mediadock.app.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val api: MediaDockApi,
    private val tokenManager: SecureTokenManager
) : MediaRepository {

    override suspend fun ensureSession(): Boolean = withContext(Dispatchers.IO) {
        if (tokenManager.isSessionValid()) return@withContext true
        try {
            val response = api.createGuestSession()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                tokenManager.saveGuestSession(
                    token = data.token,
                    sessionId = data.sessionId,
                    expiresAt = data.expiresAt,
                    quota = data.quota
                )
                return@withContext true
            }
        } catch (_: Exception) {}
        return@withContext false
    }

    override suspend fun inspectMedia(url: String): Resource<MediaDetails> = withContext(Dispatchers.IO) {
        ensureSession()
        try {
            val response = api.inspectMedia(InspectMediaRequestDto(url))
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val details = MediaDetails(
                    id = dto.id,
                    platform = dto.platform,
                    url = dto.url,
                    title = dto.title,
                    description = dto.description,
                    thumbnail = dto.thumbnail,
                    durationSeconds = dto.durationSeconds,
                    uploader = dto.uploader,
                    uploaderUrl = dto.uploaderUrl,
                    viewCount = dto.viewCount,
                    videoQualities = dto.qualities?.video ?: emptyList(),
                    audioQualities = dto.qualities?.audio ?: emptyList(),
                    videoFormats = dto.formats?.video ?: emptyList(),
                    audioFormats = dto.formats?.audio ?: emptyList(),
                    subtitles = dto.subtitles.map { SubtitleTrack(it.lang, it.name, it.auto) }
                )
                Resource.Success(details)
            } else {
                val errMsg = response.errorBody()?.string() ?: "Failed to inspect media: HTTP ${response.code()}"
                Resource.Error(errMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network connection failed", e)
        }
    }

    override suspend fun extractImages(url: String): Resource<List<ExtractedImage>> = withContext(Dispatchers.IO) {
        ensureSession()
        try {
            val response = api.extractImages(url)
            if (response.isSuccessful && response.body() != null) {
                val images = response.body()!!.images.map { dto ->
                    ExtractedImage(
                        id = dto.id,
                        url = dto.url,
                        title = dto.title,
                        alt = dto.alt,
                        width = dto.width,
                        height = dto.height,
                        format = dto.format ?: "jpg",
                        source = dto.source
                    )
                }
                Resource.Success(images)
            } else {
                val errMsg = response.errorBody()?.string() ?: "Image extraction failed: HTTP ${response.code()}"
                Resource.Error(errMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to reach target webpage", e)
        }
    }
}
