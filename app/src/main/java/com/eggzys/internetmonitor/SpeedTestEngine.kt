package com.eggzys.internetmonitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class SpeedTestResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Long
)

class SpeedTestEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // All download URLs from multiple servers
    private val downloadUrls = listOf(
        "https://speed.cloudflare.com/__down?bytes=10000000",
        "https://speed.cloudflare.com/__down?bytes=10000000",
        "https://speed.cloudflare.com/__down?bytes=10000000",
        "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Tsunami_by_hokusai_19th_century.jpg/800px-Tsunami_by_hokusai_19th_century.jpg"
    )

    // Ping URLs
    private val pingUrls = listOf(
        "https://speed.cloudflare.com/__down?bytes=1",
        "https://www.google.com/generate_204"
    )

    var onProgress: ((phase: String, speedMbps: Float) -> Unit)? = null
    var onPingResult: ((pingMs: Long) -> Unit)? = null
    var onDownloadResult: ((speedMbps: Double) -> Unit)? = null
    var onUploadResult: ((speedMbps: Double) -> Unit)? = null

    suspend fun runTest(): SpeedTestResult = withContext(Dispatchers.IO) {
        // 1. Ping test - multiple servers
        onProgress?.invoke("MEASURING PING...", 0f)
        val ping = measurePing()
        onPingResult?.invoke(ping)

        // 2. Download test - all servers in parallel
        onProgress?.invoke("TESTING DOWNLOAD...", 0f)
        val downloadSpeed = measureDownload()
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

    private fun measurePing(): Long {
        val allPings = mutableListOf<Long>()
        for (url in pingUrls) {
            repeat(4) {
                try {
                    val start = System.nanoTime()
                    val request = Request.Builder().url(url).head().build()
                    client.newCall(request).execute().use { response ->
                        val elapsed = (System.nanoTime() - start) / 1_000_000
                        if (response.code in 200..399) {
                            allPings.add(elapsed)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        if (allPings.isEmpty()) return -1L
        val sorted = allPings.sorted()
        val trimmed = if (sorted.size > 2) sorted.subList(1, sorted.size - 1) else sorted
        return trimmed.average().toLong()
    }

    private suspend fun measureDownload(): Double = coroutineScope {
        try {
            val startTime = System.nanoTime()
            var totalBytes = 0L

            // Download from ALL servers in parallel
            val jobs = downloadUrls.map { url ->
                async(Dispatchers.IO) {
                    downloadFile(url) { bytes ->
                        synchronized(this@coroutineScope) {
                            totalBytes += bytes
                        }
                    }
                }
            }

            // Monitor progress
            val monitorJob = async(Dispatchers.IO) {
                while (true) {
                    kotlinx.coroutines.delay(200)
                    val elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0
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

            (totalBytes * 8) / (totalTime * 1_000_000)
        } catch (_: Exception) {
            0.0
        }
    }

    private fun downloadFile(url: String, onBytesRead: (Long) -> Unit): Long {
        var totalBytes = 0L
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return 0L
                val inputStream = response.body?.byteStream() ?: return 0L
                val buffer = ByteArray(16384)
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
            val dataSize = 4_000_000
            val data = ByteArray(dataSize)
            java.util.Random().nextBytes(data)

            onProgress?.invoke("TESTING UPLOAD...", 0f)
            val startTime = System.nanoTime()

            val request = Request.Builder()
                .url("https://httpbin.org/post")
                .post(data.toRequestBody())
                .build()

            client.newCall(request).execute().use {
                val totalTime = (System.nanoTime() - startTime) / 1_000_000_000.0
                if (totalTime < 0.1) return@withContext 0.0
                (dataSize * 8) / (totalTime * 1_000_000)
            }
        } catch (_: Exception) {
            try {
                val dataSize = 2_000_000
                val data = ByteArray(dataSize)
                java.util.Random().nextBytes(data)
                val startTime = System.nanoTime()
                val request = Request.Builder()
                    .url("https://speed.cloudflare.com/__up")
                    .post(data.toRequestBody())
                    .build()
                client.newCall(request).execute().use {
                    val totalTime = (System.nanoTime() - startTime) / 1_000_000_000.0
                    if (totalTime < 0.1) return@withContext 0.0
                    (dataSize * 8) / (totalTime * 1_000_000)
                }
            } catch (_: Exception) {
                0.0
            }
        }
    }
}