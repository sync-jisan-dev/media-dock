package com.mediadock.app.domain.repository

import com.mediadock.app.core.common.Resource
import com.mediadock.app.domain.model.ExtractedImage
import com.mediadock.app.domain.model.MediaDetails

interface MediaRepository {
    suspend fun ensureSession(): Boolean
    suspend fun inspectMedia(url: String): Resource<MediaDetails>
    suspend fun extractImages(url: String): Resource<List<ExtractedImage>>
}
