package com.openwrt.mgr.data

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Tiny HTTP helper (replaces OkHttp) for local OpenWrt management.
 * Trusts all TLS certs (routers often use self-signed HTTPS).
 */
class HttpTransport(
    private val connectTimeoutMs: Int = 8_000,
    private val readTimeoutMs: Int = 20_000
) {
    data class HttpResponse(
        val code: Int,
        val body: String,
        val headerFields: Map<String, List<String>>
    ) {
        val isSuccessful: Boolean get() = code in 200..299
        fun headers(name: String): List<String> {
            val key = headerFields.keys.firstOrNull { it != null && it.equals(name, ignoreCase = true) }
            return key?.let { headerFields[it] } ?: emptyList()
        }
    }

    init {
        installTrustAll()
    }

    fun getBytes(url: String, extraHeaders: Map<String, String> = emptyMap()): Pair<Int, ByteArray> {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            doInput = true
            instanceFollowRedirects = true
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("User-Agent", "OpenWrtMGR/1.0")
            extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream ?: conn.inputStream else conn.inputStream
            val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
            return code to bytes
        } finally {
            conn.disconnect()
        }
    }
    fun postJson(url: String, json: String, extraHeaders: Map<String, String> = emptyMap()): HttpResponse {
        return post(url, json.toByteArray(Charsets.UTF_8), "application/json; charset=utf-8", extraHeaders)
    }

    fun postForm(url: String, form: Map<String, String>, extraHeaders: Map<String, String> = emptyMap()): HttpResponse {
        val encoded = form.entries.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
        return post(url, encoded.toByteArray(Charsets.UTF_8), "application/x-www-form-urlencoded; charset=utf-8", extraHeaders)
    }

    fun post(
        url: String,
        body: ByteArray,
        contentType: String,
        extraHeaders: Map<String, String> = emptyMap()
    ): HttpResponse {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doInput = true
            doOutput = true
            instanceFollowRedirects = true
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("User-Agent", "OpenWrtMGR/1.0")
            setRequestProperty("Content-Length", body.size.toString())
            extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream ?: conn.inputStream else conn.inputStream
            val text = stream?.use { inp ->
                BufferedReader(InputStreamReader(inp, Charsets.UTF_8)).readText()
            }.orEmpty()
            @Suppress("UNCHECKED_CAST")
            val headers = conn.headerFields.filterKeys { it != null } as Map<String, List<String>>
            return HttpResponse(code, text, headers)
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        @Volatile private var installed = false

        private fun installTrustAll() {
            if (installed) return
            synchronized(this) {
                if (installed) return
                val trustAll = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                }
                val ctx = SSLContext.getInstance("TLS")
                ctx.init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(ctx.socketFactory)
                HttpsURLConnection.setDefaultHostnameVerifier(HostnameVerifier { _, _ -> true })
                installed = true
            }
        }
    }
}
