package me.urfate.yomuki.adapter.pages

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*


internal class ProgressResponseBody(
    private val responseBody: ResponseBody,
    progressListener: ProgressListener
) :
    ResponseBody() {
    private val progressListener: ProgressListener
    private var bufferedSource: BufferedSource? = null

    init {
        this.progressListener = progressListener
    }

    override fun contentType(): MediaType? {
        return responseBody.contentType()
    }

    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }
        return bufferedSource as BufferedSource
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = sink.let { super.read(it, byteCount) }
                // read() returns the number of bytes read, or -1 if this source is exhausted.
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                progressListener.update(
                    totalBytesRead,
                    responseBody.contentLength(),
                    bytesRead == -1L
                )
                return bytesRead
            }
        }
    }
}

internal interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}