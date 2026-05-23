package com.eggzys.internetmonitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class SpeedTestResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Long
)

data class SpeedTestServer(
    val name: String,
    val downloadUrls: List<String>,
    val pingUrl: String
) {
    companion object {
        val CLOUDFLARE = SpeedTestServer(
            name = "Cloudflare",
            downloadUrls = listOf(
                "https://speed.cloudflare.com/__down?bytes=10000000",
                "https://speed.cloudflare.com/__down?bytes=10000000",
                "https://speed.cloudflare.com/__down?bytes=10000000"
            ),
            pingUrl = "https://speed.cloudflare.com/__down?bytes=1"
        )

        val GOOGLE = SpeedTestServer(
            name = "Google",
            downloadUrls = listOf(
                "https://www.google.com/favicon.ico",
                "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Tsunami_by_hokusai_19th_century.jpg/800px-Tsunami_by_hokusai_19th_century.jpg"
            ),
            pingUrl = "https://www.google.com/generate_204"
        )

        val ALL = listOf(CLOUDFLARE, GOOGLE)
    }
}

class SpeedTestEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    var onProgress: ((phase: String, speedMbps: Float) -> Unit)? = null
    var onPingResult: ((pingMs: Long) -> Unit)? = null
    var onDownloadResult: ((speedMbps: Double) -> Unit)? = null
    var onUploadResult: ((speedMbps: Double) -> Unit)? = null

    suspend fun runTest(server: SpeedTestServer): SpeedTestResult = withContext(Dispatchers.IO) {
        // 1. Ping test
        onProgress?.invoke("MEASURING PING...", 0f)
        val ping = measurePing(server.pingUrl)
        onPingResult?.invoke(ping)

        // 2. Download test - multiple parallel streams
        onProgress?.invoke("TESTING DOWNLOAD...", 0f)
        val downloadSpeed = measureDownload(server.downloadUrls)
        onDownloadResult?.invoke(downloadSpeed)

        // 3. Upload test
        onProgress?.invoke("TESTING UPLOAD...", 0f)
        val uploadSpeed = measureUpload()
        onUploadResult?.invoke(uploadSpeed)

        onProgress?.invoke("COMPLETE", 0f)

        SpeedTestResult(
            downloadMbps = downloadSpeed,
            uploadMbps = uploadSpeed,
            pingMs = ping
        )
    }

    private fun measurePing(url: String): Long {
        val pings = mutableListOf<Long>()
        repeat(7) {
            try {
                val start = System.nanoTime()
                val request = Request.Builder()
                    .url(url)
                    .head()
                    .build()
                client.newCall(request).execute().use { response ->
                    val elapsed = (System.nanoTime() - start) / 1_000_000
                    if (response.code in 200..399) {
                        pings.add(elapsed)
                    }
                }
            } catch (_: Exception) {}
        }
        if (pings.isEmpty()) return -1L
        // Remove highest and lowest, take average
        val sorted = pings.sorted()
        val trimmed = if (sorted.size > 2) sorted.subList(1, sorted.size - 1) else sorted
        return trimmed.average().toLong()
    }

    private suspend fun measureDownload(urls: List<String>): Double = coroutineScope {
        try {
            val startTime = System.nanoTime()
            var totalBytes = 0L

            // Download multiple files in parallel
            val jobs = urls.map { url ->
                async(Dispatchers.IO) {
                    downloadFile(url) { bytes ->
                        synchronized(this@coroutineScope) {
                            totalBytes += bytes
                        }
                    }
                }
            }

            // Monitor progress while downloading
            val monitorJob = async(Dispatchers.IO) {
                while (true) {
                    kotlinx.coroutines.delay(200)
                    val now = System.nanoTime()
                    val elapsed = (now - startTime) / 1_000_000_000.0
                    if (elapsed > 0.1) {
                        val speedMbps = (totalBytes * 8) / (elapsed * 1_000_000)
                        onProgress?.invoke("TESTING DOWNLOAD...", speedMbps.toFloat())
                    }
                }
            }

            jobs.awaitAll()
            monitorJob.cancel()

            val totalTime = (System.nanoTime() - startTime) / 1_000_000_000.0
            if (totalTime < 0.1 || totalBytes == 0L) return@coroutineScope 0.0

            val speedMbps = (totalBytes * 8) / (totalTime * 1_000_000)
            speedMbps
        } catch (e: Exception) {
            0.0
        }
    }

    private fun downloadFile(url: String, onBytesRead: (Long) -> Unit): Long {
        var totalBytes = 0L
        try {
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return 0L

                val inputStream = response.body?.byteStream() ?: return 0L
                val buffer = ByteArray(16384) // 16KB buffer
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                    onBytesRead(bytesRead.toLong())
                }
            }
        } catch (_: Exception) {}
        return totalBytes
    }

    private suspend fun measureUpload(): Double = withContext(Dispatchers.IO) {
        try {
            // Generate 4MB of random data for upload
            val dataSize = 4_000_000
            val data = ByteArray(dataSize)

            // Use a faster random fill
            val random = java.util.Random()
            var i = 0
            while (i < dataSize) {
                val chunk = minOf(1024, dataSize - i)
                val bytes = ByteArray(chunk)
                random.nextBytes(bytes)
                System.arraycopy(bytes, 0, data, i, chunk)
                i += chunk
            }

            onProgress?.invoke("TESTING UPLOAD...", 0f)

            val startTime = System.nanoTime()

            // Use httpbin.org for upload test
            val request = Request.Builder()
                .url("https://httpbin.org/post")
                .post(data.toRequestBody())
                .build()

            client.newCall(request).execute().use {
                val totalTime = (System.nanoTime() - startTime) / 1_000_000_000.0
                if (totalTime < 0.1) return@withContext 0.0
                val speedMbps = (dataSize * 8) / (totalTime * 1_000_000)
                speedMbps
            }
        } catch (e: Exception) {
            // Fallback: measure upload speed via Cloudflare
            try {
                measureUploadFallback()
            } catch (_: Exception) {
                0.0
            }
        }
    }

    private fun measureUploadFallback(): Double {
        val dataSize = 2_000_000
        val data = ByteArray(dataSize).apply {
            java.util.Random().nextBytes(this)
        }

        val startTime = System.nanoTime()

        // POST to Cloudflare speed test endpoint
        val request = Request.Builder()
            .url("https://speed.cloudflare.com/__up")
            .post(data.toRequestBody())
            .build()

        client.newCall(request).execute().use {
            val totalTime = (System.nanoTime() - startTime) / 1_000_000_000.0
            if (totalTime < 0.1) return 0.0
            return (dataSize * 8) / (totalTime * 1_000_000)
        }
    }
}