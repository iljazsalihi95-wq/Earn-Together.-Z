package com.weearntogether.play

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** All cloud operations run on this serial worker, never the UI thread. */
object CloudWorker { val executor = Executors.newSingleThreadExecutor() }
class BackendException(val code: String, message: String) : Exception(message)
class BackendClient {
    companion object {
        const val ENDPOINT = "https://script.google.com/macros/s/AKfycbw4GAz9_Om3YVU1Eil30WgEHvFkvW3ygo4sGqXNG8QBOGitUXGOkXWbntpJO4JybXPr/exec"
    }
    fun request(action: String, data: JSONObject = JSONObject()): JSONObject {
        val payload = JSONObject(data.toString()).put("action", action).toString().toByteArray(Charsets.UTF_8)
        var url = URL(ENDPOINT)
        var body: ByteArray? = payload
        repeat(5) {
            require(url.protocol == "https" && url.host in setOf("script.google.com", "script.googleusercontent.com"))
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 20_000
                connection.readTimeout = 150_000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")
                val outgoing = body
                if (outgoing != null) {
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(outgoing) }
                }
                val code = connection.responseCode
                if (code in listOf(301, 302, 303)) {
                    url = URL(url, connection.getHeaderField("Location") ?: throw Exception("Invalid redirect"))
                    body = null // Apps Script redirects its JSON response to a GET content URL.
                } else {
                    if (code != 200) throw BackendException("NETWORK", "Server unavailable ($code). Try again.")
                    val text = connection.inputStream.bufferedReader().use { reader ->
                        val out = StringBuilder(); val buffer = CharArray(4096)
                        while (true) { val n = reader.read(buffer); if (n < 0) break; out.append(buffer, 0, n); if (out.length > 250_000) throw Exception("Response too large") }
                        out.toString()
                    }
                    val result = try { JSONObject(text) } catch (_: Exception) { throw BackendException("NETWORK", "Server did not return JSON. Try again.") }
                    if (!result.optBoolean("success")) throw BackendException(result.optString("error", "SERVER_ERROR"), result.optString("message", "Request failed"))
                    return result
                }
            } finally { connection.disconnect() }
        }
        throw BackendException("NETWORK", "Too many server redirects")
    }
}
