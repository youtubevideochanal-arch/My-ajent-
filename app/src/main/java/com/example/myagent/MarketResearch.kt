package com.example.myagent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * MY AGENT - Market Research
 * Kaam: market research data lena, check karna aur aage bhejna.
 * Trade lene ya na lene ka faisla yeh file nahi karegi.
 */
class MarketResearch {

    data class Request(
        val symbol: String,
        val market: String = "NSE",
        val timeframe: String = "1D",
        val question: String = ""
    )

    data class Evidence(
        val source: String,
        val url: String,
        val time: String,
        val type: String,
        val content: String,
        val verified: Boolean = false
    )

    data class Bundle(
        val request: Request,
        val evidence: List<Evidence>,
        val warnings: List<String>
    )

    fun createRequest(
        symbol: String,
        market: String = "NSE",
        timeframe: String = "1D",
        question: String = ""
    ): Request {
        return Request(
            symbol = symbol.trim().uppercase(),
            market = market.trim().uppercase(),
            timeframe = timeframe.trim(),
            question = question.trim()
        )
    }

    /**
     * Configured HTTPS JSON API se data leta hai.
     * Actual provider endpoint abhi configure karna baaki hai.
     */
    suspend fun fetch(
        endpoint: String,
        token: String? = null
    ): Result<JSONObject> = withContext(Dispatchers.IO) {

        var connection: HttpURLConnection? = null

        try {
            val url = URL(endpoint)

            require(url.protocol == "https") {
                "Sirf HTTPS endpoint allowed hai."
            }

            connection =
                (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 15000

                    setRequestProperty(
                        "Accept",
                        "application/json"
                    )

                    if (!token.isNullOrBlank()) {
                        setRequestProperty(
                            "Authorization",
                            "Bearer $token"
                        )
                    }
                }

            val status = connection.responseCode

            if (status !in 200..299) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Data provider HTTP error: $status"
                    )
                )
            }

            val body = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            Result.success(JSONObject(body))

        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    fun validate(
        request: Request,
        evidence: List<Evidence>
    ): Bundle {

        val warnings = mutableListOf<String>()

        if (request.symbol.isBlank()) {
            warnings.add("Market symbol missing hai.")
        }

        if (evidence.isEmpty()) {
            warnings.add("Market data available nahi hai.")
        }

        if (evidence.any { !it.verified }) {
            warnings.add(
                "Kuch sources independently verify nahi hue."
            )
        }

        if (evidence.any { it.time.isBlank() }) {
            warnings.add("Kuch data ka timestamp missing hai.")
        }

        return Bundle(
            request = request,
            evidence = evidence,
            warnings = warnings
        )
    }

    fun makeAnalysisInput(bundle: Bundle): String {

        val evidenceText = bundle.evidence.joinToString(
            separator = "\n\n"
        ) { item ->
            """
            Source: ${item.source}
            URL: ${item.url}
            Time: ${item.time}
            Type: ${item.type}
            Verified: ${item.verified}
            Content: ${item.content}
            """.trimIndent()
        }

        val warningsText = if (bundle.warnings.isEmpty()) {
            "No warnings."
        } else {
            bundle.warnings.joinToString("\n") { "- $it" }
        }

        return """
            MY AGENT MARKET RESEARCH

            Symbol: ${bundle.request.symbol}
            Market: ${bundle.request.market}
            Timeframe: ${bundle.request.timeframe}
            Question: ${bundle.request.question}

            Research time: ${Instant.now()}

            Evidence:
            $evidenceText

            Warnings:
            $warningsText

            Rules:
            - Hindi mein analysis karo.
            - Fake price, news ya source mat banao.
            - Facts aur opinions alag rakho.
            - Purane data ko latest mat batao.
            - Data insufficient ho to clearly batao.
            - Yeh data analysis ke liye hai, automatic trade
              execute karne ki permission nahi hai.
        """.trimIndent()
    }
}
