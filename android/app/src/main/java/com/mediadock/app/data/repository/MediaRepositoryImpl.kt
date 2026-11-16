package com.mediadock.app.data.repository

import com.mediadock.app.core.common.Resource
import com.mediadock.app.core.network.MediaDockApi
import com.mediadock.app.core.network.BackendConnection
import com.mediadock.app.core.security.SecureTokenManager
import com.mediadock.app.data.remote.dto.InspectMediaRequestDto
import com.mediadock.app.domain.model.ExtractedImage
import com.mediadock.app.domain.model.MediaDetails
import com.mediadock.app.domain.model.SubtitleTrack
import com.mediadock.app.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val api: MediaDockApi,
    private val tokenManager: SecureTokenManager,
    private val connection: BackendConnection
) : MediaRepository {
    private val sessionMutex = Mutex()

    override suspend fun ensureSession(): Boolean = withContext(Dispatchers.IO) {
        sessionMutex.withLock {
        if (!connection.awaitReady()) return@withLock false
        if (tokenManager.isSessionValid()) return@withLock true
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
                return@withLock true
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {}
        false
        }
    }

    override suspend fun inspectMedia(url: String): Resource<MediaDetails> = withContext(Dispatchers.IO) {
        if (!ensureSession()) return@withContext Resource.Error("The server is still waking up or is unavailable. Check your internet connection and try again shortly.")
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
                val rawErr = response.errorBody()?.string()
                val errMsg = formatApiError(rawErr, response.code())
                Resource.Error(errMsg)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val friendlyMessage = formatNetworkError(e)
            Resource.Error(friendlyMessage, e)
        }
    }

    private fun formatApiError(rawBody: String?, statusCode: Int): String {
        if (rawBody.isNullOrBlank()) return "MediaDock server returned an empty error (HTTP $statusCode)."
        return try {
            val json = JSONObject(rawBody)
            val code = json.optString("code")
            val message = json.optString("message")

            when {
                code == "EXTRACTION_FAILED" || message.contains("Failed to extract", ignoreCase = true) || message.contains("yt-dlp", ignoreCase = true) ->
                    "MediaDock could not extract media from this link. The video may be restricted, private, age-gated, or unavailable."
                code == "UNSUPPORTED_PLATFORM" || message.contains("not a supported platform", ignoreCase = true) ->
                    "This platform or link format is not supported yet. Please check the URL or try another media link."
                code == "INVALID_URL" || message.contains("valid http", ignoreCase = true) ->
                    "Invalid link format. Please paste a full web link (starting with https://)."
                code == "RATE_LIMITED" ->
                    "Too many requests. Please wait a moment before trying again."
                message.isNotBlank() && !message.startsWith("{") && message.length < 160 ->
                    message
                else ->
                    "Unable to extract media (HTTP $statusCode). Please verify the link and try again."
            }
        } catch (_: Exception) {
            if (rawBody.length > 180 || rawBody.startsWith("{")) {
                "Unable to extract media (HTTP $statusCode). Please verify the link and try again."
            } else {
                rawBody
            }
        }
    }

    private fun formatNetworkError(e: Exception): String {
        val raw = e.message.orEmpty()
        return when {
            raw.contains("10.0.2.2") || raw.contains("failed to connect") || e is java.net.ConnectException ->
                "Cannot reach MediaDock. Check your internet connection and the server address in Settings."
            e is java.net.SocketTimeoutException ->
                "The server took too long to respond. A free server may need time to wake up. Try again shortly."
            else -> e.localizedMessage ?: "Network connection failed"
        }
    }

    override suspend fun extractImages(url: String): Resource<List<ExtractedImage>> = withContext(Dispatchers.IO) {
        if (!ensureSession()) return@withContext Resource.Error("The server is still waking up or is unavailable. Try again shortly.")
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
                val rawErr = response.errorBody()?.string()
                val errMsg = formatApiError(rawErr, response.code())
                Resource.Error(errMsg)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(formatNetworkError(e), e)
        }
    }
}
