package com.example.remotefilebox


import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface APIService {
    @Multipart
    @POST("uploads.php")
    fun uploadSelectedImages(
        @Part images: List<MultipartBody.Part>
    ): Call<UploadResponse>

    @GET("list-uploads.php")
    fun uploadedFileList() : Call<UploadList>
}