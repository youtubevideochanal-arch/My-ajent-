package com.example.myagent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApiManager(
    private val prefs: AgentPreferences
) {

    private val apiNames = listOf(
        "GROQ_API",
        "VENICE_API",
        "GEMINI_API",
        "FUTURE_API"
    )

    suspend fun getAIResponse(prompt: String): String {
        if (prompt.isBlank()) {
            return "Boss, pehle apna message likho."
        }

        val errors = mutableListOf<String>()

        for (apiName in apiNames) {
            val apiKey = prefs.getApiKey(apiName)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

            if (!prefs.isApiEnabled(apiName) || apiKey == null) {
                continue
            }

            try {
                val answer = withTimeout(30_000L) {
                    withContext(Dispatchers.IO) {
                        callActualApi(apiName, apiKey, prompt)
                    }
                }

                if (answer.isNotBlank()) {
                    return answer
                }

                errors += "$apiName: empty response"

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                // API key ya sensitive request details log nahi karte.
                errors += "$apiName: ${e.javaClass.simpleName}"
            }
        }

        return if (errors.isEmpty()) {
            "Boss, koi API enabled nahi hai ya API key save nahi hai. Settings check karo."
        } else {
            "Boss, enabled APIs se jawab nahi mila. API keys, model, quota aur internet check karo."
        }
    }

    private fun callActualApi(
        apiName: String,
        apiKey: String,
        prompt: String
    ): String {
        return when (apiName) {
            "GROQ_API" -> callOpenAiCompatibleApi(
                endpoint = "https://api.groq.com/openai/v1/chat/completions",
                apiKey = apiKey,
                model = "llama-3.3-70b-versatile",
                prompt = prompt
            )

            "VENICE_API" -> callOpenAiCompatibleApi(
                endpoint = "https://api.venice.ai/api/v1/chat/completions",
                apiKey = apiKey,
                model = "llama-3.3-70b",
                prompt = prompt
            )

            "GEMINI_API" -> callGeminiApi(
                apiKey = apiKey,
                model = "gemini-2.5-flash",
                prompt = prompt
            )

            "FUTURE_API" -> {
                throw IOException(
                    "Custom API endpoint aur model configure karna baaki hai."
                )
            }

            else -> throw IOException("Unknown API provider.")
        }
    }

    private fun callOpenAiCompatibleApi(
        endpoint: String,
        apiKey: String,
        model: String,
        prompt: String
    ): String {
        val body = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", prompt)
                )
            )
            .put("temperature", 0.3)
            .put("max_tokens", 1024)

        val response = postJson(
            endpoint = endpoint,
            apiKey = apiKey,
            body = body,
            useGoogleApiKeyHeader = false
        )

        val content = response
            .optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?.trim()

        return content
            ?.takeIf { it.isNotEmpty() }
            ?: throw IOException("AI response mein content nahi mila.")
    }

    private fun callGeminiApi(
        apiKey: String,
        model: String,
        prompt: String
    ): String {
        val endpoint =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                "$model:generateContent"

        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put("text", prompt)
                            )
                        )
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.3)
                    .put("maxOutputTokens", 1024)
            )

        val response = postJson(
            endpoint = endpoint,
            apiKey = apiKey,
            body = body,
            useGoogleApiKeyHeader = true
        )

        val candidates = response.optJSONArray("candidates")
            ?: throw IOException("Gemini ne candidates return nahi kiye.")

        val parts = candidates
            .optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?: throw IOException("Gemini response mein text nahi mila.")

        val result = buildString {
            for (i in 0 until parts.length()) {
                val text = parts.optJSONObject(i)?.optString("text")
                if (!text.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n")
                    append(text)
                }
            }
        }.trim()

        return result.takeIf { it.isNotEmpty() }
            ?: throw IOException("Gemini ka response khaali hai.")
    }

    private fun postJson(
        endpoint: String,
        apiKey: String,
        body: JSONObject,
        useGoogleApiKeyHeader: Boolean
    ): JSONObject {
        val connection = (URL(endpoint).openConnection()
            as HttpURLConnection)

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 25_000
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (useGoogleApiKeyHeader) {
                connection.setRequestProperty(
                    "x-goog-api-key",
                    apiKey
                )
            } else {
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $apiKey"
                )
            }

            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode

            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.bufferedReader()?.use {
                it.readText()
            }.orEmpty()

            if (status !in 200..299) {
                // Response body ko log nahi karte; usmein sensitive data ho sakta hai.
                throw IOException("API HTTP error: $status")
            }

            return try {
                JSONObject(responseText)
            } catch (_: Exception) {
                throw IOException("API ne valid JSON return nahi kiya.")
            }
        } finally {
            connection.disconnect()
        }
    }
}
