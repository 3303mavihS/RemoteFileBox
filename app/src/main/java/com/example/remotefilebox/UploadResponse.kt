package com.example.remotefilebox

data class UploadResponse(
    val success: Boolean,
    val files: List<String>?,
    val message: String?
)
