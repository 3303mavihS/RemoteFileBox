package com.example.remotefilebox

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.ForwardingSink
import okio.Okio
import okio.buffer
import okio.source
import java.io.File
import java.io.IOException

interface ProgressListener {
    fun onProgressUpdate(percentage: Int)
}

class ProgressRequestBody(
    private val file: File,
    private val contentType: String,
    private val listener: ProgressListener
):RequestBody() {
    override fun contentType(): MediaType? {
        return contentType.toMediaTypeOrNull()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val fileLength = file.length()
        var uploaded: Long = 0

        val source = file.source()
        val forwardingSink = object : ForwardingSink(sink) {
            @Throws(IOException::class)
            override fun write(source: okio.Buffer, byteCount: Long) {
                super.write(source, byteCount)
                uploaded += byteCount
                val progress = (100 * uploaded / fileLength).toInt()
                listener.onProgressUpdate(progress)
            }
        }

        val bufferedSink = forwardingSink.buffer()
        bufferedSink.writeAll(source)
        bufferedSink.flush()
    }
}