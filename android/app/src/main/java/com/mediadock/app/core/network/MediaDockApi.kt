package com.mediadock.app.core.network

import com.mediadock.app.data.remote.dto.CreateBatchRequestDto
import com.mediadock.app.data.remote.dto.CreateDownloadRequestDto
import com.mediadock.app.data.remote.dto.DownloadImageRequestDto
import com.mediadock.app.data.remote.dto.DownloadJobResponseDto
import com.mediadock.app.data.remote.dto.ExtractImagesResponseDto
import com.mediadock.app.data.remote.dto.GuestSessionResponseDto
import com.mediadock.app.data.remote.dto.InspectMediaRequestDto
import com.mediadock.app.data.remote.dto.MediaInfoResponseDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface MediaDockApi {

    @GET("api/health")
    suspend fun checkLiveness(): Response<Map<String, String>>

    @POST("app/v1/sessions/guest")
    suspend fun createGuestSession(): Response<GuestSessionResponseDto>

    @POST("app/v1/media/inspect")
    suspend fun inspectMedia(@Body request: InspectMediaRequestDto): Response<MediaInfoResponseDto>

    @POST("app/v1/downloads")
    suspend fun createDownload(@Body request: CreateDownloadRequestDto): Response<DownloadJobResponseDto>

    @POST("app/v1/downloads/batch")
    suspend fun createBatch(@Body request: CreateBatchRequestDto): Response<List<DownloadJobResponseDto>>

    @GET("app/v1/downloads/{id}")
    suspend fun getDownload(@Path("id") id: String): Response<DownloadJobResponseDto>

    @POST("app/v1/downloads/{id}/cancel")
    suspend fun cancelDownload(@Path("id") id: String): Response<DownloadJobResponseDto>

    @GET("app/v1/images/extract")
    suspend fun extractImages(@Query("url") url: String): Response<ExtractImagesResponseDto>

    @POST("app/v1/images/download")
    suspend fun downloadImage(@Body request: DownloadImageRequestDto): Response<DownloadJobResponseDto>

    @GET("app/v1/health")
    suspend fun checkHealth(): Response<Map<String, String>>

    @Streaming
    @GET
    suspend fun streamFile(
        @Url fileUrl: String,
        @Header("Range") rangeHeader: String? = null,
        @Header("User-Agent") userAgent: String? = null,
        @Header("Referer") referer: String? = null
    ): Response<ResponseBody>
}
