package com.eggzys.internetmonitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class InternetStateChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val cheburClient: OkHttpClient by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .dns(object : okhttp3.Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    return listOf(InetAddress.getByName("5.78.7.195"))
                }
            })
            .followRedirects(true)
            .build()
    }

    suspend fun checkState(urlGroups: UrlGroups): InternetState = coroutineScope {
        val mainCheck = async(Dispatchers.IO) { doMainCheck(urlGroups) }
        val cheburCheck = async(Dispatchers.IO) { cheburcheck() }

        val mainResult = mainCheck.await()
        val cheburResult = cheburCheck.await()

        return@coroutineScope when (mainResult) {
            InternetState.FULL_ACCESS -> InternetState.FULL_ACCESS
            InternetState.RUSSIA_ONLY -> InternetState.RUSSIA_ONLY
            InternetState.WHITELIST_ONLY -> InternetState.WHITELIST_ONLY
            InternetState.NO_INTERNET -> {
                if (cheburResult) InternetState.WHITELIST_ONLY
                else InternetState.NO_INTERNET
            }
            InternetState.UNKNOWN -> InternetState.UNKNOWN
        }
    }

    private suspend fun doMainCheck(urlGroups: UrlGroups): InternetState = coroutineScope {
        val globalCheck = async(Dispatchers.IO) {
            urlGroups.globalUrls.any { checkUrl(it) }
        }
        val russiaCheck = async(Dispatchers.IO) {
            urlGroups.russiaUrls.any { checkUrl(it) }
        }
        val whitelistCheck = async(Dispatchers.IO) {
            urlGroups.whitelistUrls.any { checkUrl(it) }
        }

        val global = globalCheck.await()
        val russia = russiaCheck.await()
        val whitelist = whitelistCheck.await()

        return@coroutineScope when {
            global -> InternetState.FULL_ACCESS
            russia -> InternetState.RUSSIA_ONLY
            whitelist -> InternetState.WHITELIST_ONLY
            else -> InternetState.NO_INTERNET
        }
    }

    suspend fun checkUrl(url: String, timeout: Long = 5000): Boolean = withContext(Dispatchers.IO) {
        try {
            val headRequest = Request.Builder()
                .url(url)
                .head()
                .build()
            val headResponse = client.newCall(headRequest).execute()
            headResponse.close()
            if (headResponse.code in 200..399) return@withContext true
        } catch (_: Exception) {
        }

        try {
            val getRequest = Request.Builder()
                .url(url)
                .build()
            val getResponse = client.newCall(getRequest).execute()
            getResponse.close()
            return@withContext getResponse.code in 200..399
        } catch (_: Exception) {
            return@withContext false
        }
    }

    suspend fun cheburcheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://ok.ru/100MB.bin")
                .header("Range", "bytes=0-65536")
                .build()
            val response = cheburClient.newCall(request).execute()
            val body = response.body
            val bytes = body?.bytes() ?: ByteArray(0)
            body?.close()
            response.close()
            return@withContext bytes.size >= 60000
        } catch (_: Exception) {
            return@withContext false
        }
    }
}
