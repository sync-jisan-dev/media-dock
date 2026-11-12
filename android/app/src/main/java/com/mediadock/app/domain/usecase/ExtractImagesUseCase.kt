package com.mediadock.app.domain.usecase

import com.mediadock.app.core.common.FormattingUtils
import com.mediadock.app.core.common.Resource
import com.mediadock.app.domain.model.ExtractedImage
import com.mediadock.app.domain.repository.MediaRepository
import javax.inject.Inject

class ExtractImagesUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(url: String): Resource<List<ExtractedImage>> {
        if (!FormattingUtils.isValidUrl(url)) {
            return Resource.Error("Please enter a valid webpage URL.")
        }
        return repository.extractImages(url.trim())
    }
}
