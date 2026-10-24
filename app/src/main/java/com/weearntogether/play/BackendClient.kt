package com.weearntogether.play

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors

/** All cloud operations run on this serial worker, never the UI thread. */
object CloudWorker { val executor = Executors.newSingleThreadExecutor() }
/** Authentication never waits behind queued game-save requests. */
object AuthWorker { val executor = Executors.newSingleThreadExecutor() }
class BackendException(val code: String, message: String) : Exception(message)
class BackendClient {
    companion object {
        private val DEADLINES = Executors.newSingleThreadScheduledExecutor()
        const val REQUEST_TIMEOUT_MS = 60_000L
        const val ENDPOINT = "https://script.google.com/macros/s/AKfycbw4GAz9_Om3YVU1Eil30WgEHvFkvW3ygo4sGqXNG8QBOGitUXGOkXWbntpJO4JybXPr/exec"
    }
    private val deadlines = DEADLINES
    fun request(action: String, data: JSONObject = JSONObject()): JSONObject {
        val payload = JSONObject(data.toString()).put("action", action).toString().toByteArray(Charsets.UTF_8)
        var url = URL(ENDPOINT)
        var body: ByteArray? = payload
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(REQUEST_TIMEOUT_MS)
        repeat(5) {
            if (Thread.currentThread().isInterrupted || System.nanoTime() >= deadline) throw timedOut()
            require(url.protocol == "https" && url.host in setOf("script.google.com", "script.googleusercontent.com"))
            val connection = url.openConnection() as HttpURLConnection
            val expired = AtomicBoolean(false)
            val remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()).coerceAtLeast(1)
            val watchdog = deadlines.schedule({ expired.set(true); connection.disconnect() }, remainingMs, TimeUnit.MILLISECONDS)
            try {
                connection.connectTimeout = minOf(15_000L, remainingMs).toInt()
                connection.readTimeout = minOf(45_000L, remainingMs).toInt()
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
                if (expired.get() || System.nanoTime() >= deadline) throw timedOut()
                if (code in listOf(301, 302, 303, 307, 308)) {
                    url = URL(url, connection.getHeaderField("Location") ?: throw Exception("Invalid redirect"))
                    if (code in listOf(301, 302, 303)) body = null // ContentService JSON is retrieved with GET; 307/308 preserve POST.
                } else {
                    if (code in listOf(401, 403)) throw BackendException("DEPLOYMENT_ACCESS", "The account service is not publicly accessible. Please contact support.")
                    if (code != 200) throw BackendException("NETWORK", "Server unavailable ($code). Try again.")
                    val text = connection.inputStream.bufferedReader().use { reader ->
                        val out = StringBuilder(); val buffer = CharArray(4096)
                        while (true) { if (expired.get() || System.nanoTime() >= deadline) throw timedOut(); val n = reader.read(buffer); if (n < 0) break; out.append(buffer, 0, n); if (out.length > 250_000) throw Exception("Response too large") }
                        out.toString()
                    }
                    val result = try { JSONObject(text) } catch (_: Exception) { throw BackendException("NETWORK", "Server did not return JSON. Try again.") }
                    if (!result.optBoolean("success")) throw BackendException(result.optString("error", "SERVER_ERROR"), result.optString("message", "Request failed"))
                    return result
                }
            } catch (e: SocketTimeoutException) {
                throw timedOut()
            } catch (e: java.io.IOException) {
                if (expired.get() || System.nanoTime() >= deadline) throw timedOut()
                throw BackendException("NETWORK", "Could not reach the account service. Check your connection and try again.")
            } finally { watchdog.cancel(false); connection.disconnect() }
        }
        throw BackendException("NETWORK", "Too many server redirects")
    }
    private fun timedOut() = BackendException("TIMEOUT", "The account service took too long. Try signing in again. If you just created an account, it may already have been saved.")
}
