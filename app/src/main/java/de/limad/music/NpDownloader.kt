package de.limad.music

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

/** HTTP bridge between NewPipeExtractor and OkHttp. */
class NpDownloader(private val client: OkHttpClient) : Downloader() {

    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val data = request.dataToSend()
        val body = when {
            data != null -> data.toRequestBody()
            method == "POST" || method == "PUT" -> ByteArray(0).toRequestBody()
            else -> null
        }

        val builder = okhttp3.Request.Builder()
            .url(request.url())
            .method(method, body)
            .header("User-Agent", USER_AGENT)

        for ((name, values) in request.headers()) {
            builder.removeHeader(name)
            values.forEach { builder.addHeader(name, it) }
        }

        client.newCall(builder.build()).execute().use { r ->
            if (r.code == 429) throw ReCaptchaException("reCaptcha challenge requested", request.url())
            val text = r.body?.string() ?: ""
            return Response(r.code, r.message, r.headers.toMultimap(), text, r.request.url.toString())
        }
    }
}
