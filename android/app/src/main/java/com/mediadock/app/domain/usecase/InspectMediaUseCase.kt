package com.mediadock.app.domain.usecase

import com.mediadock.app.core.common.FormattingUtils
import com.mediadock.app.core.common.Resource
import com.mediadock.app.domain.model.MediaDetails
import com.mediadock.app.domain.repository.MediaRepository
import javax.inject.Inject

class InspectMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(url: String): Resource<MediaDetails> {
        if (!FormattingUtils.isValidUrl(url)) {
            return Resource.Error("Please enter a valid HTTP or HTTPS media link.")
        }
        return repository.inspectMedia(url.trim())
    }
}
