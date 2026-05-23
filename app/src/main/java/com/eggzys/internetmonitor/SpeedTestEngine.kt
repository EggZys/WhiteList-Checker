package com.eggzys.internetmonitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

data class SpeedTestResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Long
)

data class SpeedTestServer(
    val name: String,
    val downloadUrl: String,
    val uploadUrl: String,
    val pingUrl: String
) {
    companion object {
        val CLOUDFLARE = SpeedTestServer(
            name = "Cloudflare",
            downloadUrl = "https://speed.cloudflare.com/__down?bytes=25000000",
            uploadUrl = "https://speed.cloudflare.com/__up",
            pingUrl = "https://1.1.1.1"
        )

        val GOOGLE = SpeedTestServer(
            name = "Google",
            downloadUrl = "https://www.google.com/images/phd/px.gif",
            uploadUrl = "https://www.google.com/generate_204",
            pingUrl = "https://www.google.com/generate_204"
        )

        val ALL = listOf(CLOUDFLARE, GOOGLE)
    }
}

class SpeedTestEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    var onProgress: ((phase: String, speedMbps: Float) -> Unit)? = null
    var onPingResult: ((pingMs: Long) -> Unit)? = null
    var onDownloadResult: ((speedMbps: Double) -> Unit)? = null
    var onUploadResult: ((speedMbps: Double) -> Unit)? = null

    suspend fun runTest(server: SpeedTestServer): SpeedTestResult = withContext(Dispatchers.IO) {
        // 1. Ping test
        onProgress?.invoke("Measuring ping...", 0f)
        val ping = measurePing(server.pingUrl)
        onPingResult?.invoke(ping)

        // 2. Download test
        onProgress?.invoke("Testing download...", 0f)
        val downloadSpeed = measureDownload(server.downloadUrl)
        onDownloadResult?.invoke(downloadSpeed)

        // 3. Upload test
        onProgress?.invoke("Testing upload...", 0f)
        val uploadSpeed = measureUpload(server.uploadUrl)
        onUploadResult?.invoke(uploadSpeed)

        onProgress?.invoke("Complete", 0f)

        SpeedTestResult(
            downloadMbps = downloadSpeed,
            uploadMbps = uploadSpeed,
            pingMs = ping
        )
    }

    private fun measurePing(url: String): Long {
        val pings = mutableListOf<Long>()
        repeat(5) {
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
        return if (pings.isNotEmpty()) pings.sorted().drop(1).dropLast(1).average().toLong() else -1L
    }

    private fun measureDownload(url: String): Double {
        return try {
            val request = Request.Builder()
                .url(url)
                .build()

            val startTime = System.nanoTime()
            var totalBytes = 0L
            var lastUpdateTime = startTime
            var lastBytes = 0L

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code ${response.code}")

                val inputStream = response.body?.byteStream() ?: throw IOException("Empty body")
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                    val now = System.nanoTime()

                    // Update progress every 200ms
                    if (now - lastUpdateTime > 200_000_000) {
                        val elapsedSec = (now - lastUpdateTime) / 1_000_000_000.0
                        val bytesInPeriod = totalBytes - lastBytes
                        val speedMbps = (bytesInPeriod * 8) / (elapsedSec * 1_000_000)
                        onProgress?.invoke("Testing download...", speedMbps.toFloat())

                        lastUpdateTime = now
                        lastBytes = totalBytes
                    }
                }
            }

            val totalTime = (System.nanoTime() - startTime) / 1_000_000_000.0
            val speedMbps = (totalBytes * 8) / (totalTime * 1_000_000)
            speedMbps
        } catch (e: Exception) {
            0.0
        }
    }

    private fun measureUpload(url: String): Double {
        return try {
            // Generate random data for upload (5MB)
            val dataSize = 5_000_000
            val data = ByteArray(dataSize).apply {
                SecureRandom().nextBytes(this)
            }

            val startTime = System.nanoTime()

            val request = Request.Builder()
                .url(url)
                .post(data.toRequestBody())
                .build()

            client.newCall(request).execute().use {
                val totalTime = (System.nanoTime() - startTime) / 1_000_000_000.0
                val speedMbps = (dataSize * 8) / (totalTime * 1_000_000)
                speedMbps
            }
        } catch (e: Exception) {
            0.0
        }
    }
}