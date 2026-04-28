package com.sojakothy.music

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class SojaKothyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        initNewPipe()
    }

    private fun initNewPipe() {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        NewPipe.init(object : Downloader() {
            override fun execute(request: Request): Response {
                val requestBuilder = okhttp3.Request.Builder()
                    .url(request.url())
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )

                // Copy all request headers
                request.headers().forEach { (key, values) ->
                    values.forEach { value -> requestBuilder.addHeader(key, value) }
                }

                // Attach body if present (POST requests)
                val bodyData = request.dataToSend()
                if (bodyData != null) {
                    val contentType = request.headers()["Content-Type"]
                        ?.firstOrNull() ?: "application/x-www-form-urlencoded"
                    requestBuilder.post(bodyData.toRequestBody(contentType.toMediaType()))
                }

                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                return Response(
                    response.code,
                    response.message,
                    response.headers.toMultimap(),
                    responseBody,
                    response.request.url.toString()
                )
            }
        })
    }
}
