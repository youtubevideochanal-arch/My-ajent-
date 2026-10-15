package com.example.myagent

import kotlin.math.abs

/**
 * MY AGENT - Chart Analysis
 *
 * Kaam:
 * 1. OHLC candle data analyse karna.
 * 2. Trend aur moving averages calculate karna.
 * 3. RSI calculate karna.
 * 4. Support/resistance levels estimate karna.
 *
 * Yeh module trade execute nahi karta.
 */
class ChartAnalysis {

    data class Candle(
        val time: String,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double,
        val volume: Double = 0.0
    )

    data class ChartReport(
        val trend: String,
        val lastClose: Double?,
        val sma20: Double?,
        val sma50: Double?,
        val rsi14: Double?,
        val support: Double?,
        val resistance: Double?,
        val warnings: List<String>
    )

    /**
     * Candle data ki basic validity check karta hai.
     */
    private fun isValid(candle: Candle): Boolean {
        return candle.open > 0 &&
            candle.high > 0 &&
            candle.low > 0 &&
            candle.close > 0 &&
            candle.high >= candle.low &&
            candle.high >= candle.open &&
            candle.high >= candle.close &&
            candle.low <= candle.open &&
            candle.low <= candle.close &&
            candle.volume >= 0
    }

    private fun sma(
        candles: List<Candle>,
        period: Int
    ): Double? {
        if (candles.size < period) return null

        return candles.takeLast(period)
            .map { it.close }
            .average()
    }

    /**
     * RSI ke liye simple average gain/loss method.
     * Period 14 ke liye kam-se-kam 15 closing prices chahiye.
     */
    private fun calculateRsi(
        candles: List<Candle>,
        period: Int = 14
    ): Double? {

        if (candles.size < period + 1) return null

        val closes = candles.map { it.close }
        var gains = 0.0
        var losses = 0.0

        for (i in closes.size - period until closes.size) {
            val change = closes[i] - closes[i - 1]

            if (change > 0) {
                gains += change
            } else {
                losses += abs(change)
            }
        }

        val averageGain = gains / period
        val averageLoss = losses / period

        if (averageGain == 0.0 && averageLoss == 0.0) {
            return 50.0
        }

        if (averageLoss == 0.0) return 100.0
        if (averageGain == 0.0) return 0.0

        val relativeStrength = averageGain / averageLoss

        return 100.0 - (100.0 / (1.0 + relativeStrength))
    }

    /**
     * Pichhle candles ke low/high se simple levels nikalta hai.
     * Ye exact institutional support/resistance nahi hain.
     */
    private fun support(
        candles: List<Candle>,
        lookback: Int = 20
    ): Double? {
        if (candles.isEmpty()) return null

        return candles.takeLast(lookback)
            .minOf { it.low }
    }

    private fun resistance(
        candles: List<Candle>,
        lookback: Int = 20
    ): Double? {
        if (candles.isEmpty()) return null

        return candles.takeLast(lookback)
            .maxOf { it.high }
    }

    fun analyze(input: List<Candle>): ChartReport {

        val warnings = mutableListOf<String>()

        if (input.isEmpty()) {
            return ChartReport(
                trend = "UNKNOWN",
                lastClose = null,
                sma20 = null,
                sma50 = null,
                rsi14 = null,
                support = null,
                resistance = null,
                warnings = listOf("Koi candle data nahi diya gaya.")
            )
        }

        val candles = input.sortedBy { it.time }

        if (candles.any { !isValid(it) }) {
            warnings.add(
                "Kuch candles invalid hain; data source check karein."
            )

            return ChartReport(
                trend = "UNKNOWN",
                lastClose = candles.last().close,
                sma20 = null,
                sma50 = null,
                rsi14 = null,
                support = null,
                resistance = null,
                warnings = warnings
            )
        }

        if (candles.size < 50) {
            warnings.add(
                "50 candles se kam data hai; SMA50 available nahi hoga."
            )
        }

        val sma20Value = sma(candles, 20)
        val sma50Value = sma(candles, 50)
        val rsiValue = calculateRsi(candles)

        val trend = when {
            sma20Value != null &&
                sma50Value != null &&
                sma20Value > sma50Value -> "UPTREND"

            sma20Value != null &&
                sma50Value != null &&
                sma20Value < sma50Value -> "DOWNTREND"

            else -> "UNKNOWN"
        }

        if (sma20Value == null) {
            warnings.add("SMA20 ke liye kam-se-kam 20 candles chahiye.")
        }

        if (rsiValue == null) {
            warnings.add("RSI14 ke liye kam-se-kam 15 closing prices chahiye.")
        }

        warnings.add(
            "Indicators historical data par based hain; " +
                "yeh future price ya profit ki guarantee nahi dete."
        )

        return ChartReport(
            trend = trend,
            lastClose = candles.last().close,
            sma20 = sma20Value,
            sma50 = sma50Value,
            rsi14 = rsiValue,
            support = support(candles),
            resistance = resistance(candles),
            warnings = warnings
        )
    }

    /**
     * Report ko debate/analysis agents ke liye text mein badalta hai.
     */
    fun makeAnalysisInput(report: ChartReport): String {
        return """
            MY AGENT - CHART ANALYSIS

            Trend: ${report.trend}
            Last close: ${report.lastClose ?: "Unavailable"}
            SMA20: ${report.sma20 ?: "Unavailable"}
            SMA50: ${report.sma50 ?: "Unavailable"}
            RSI14: ${report.rsi14 ?: "Unavailable"}
            Estimated support: ${report.support ?: "Unavailable"}
            Estimated resistance: ${report.resistance ?: "Unavailable"}

            Warnings:
            ${report.warnings.joinToString("\n") { "- $it" }}

            Instructions:
            - Missing values ko invent mat karo.
            - Indicators ko certainty mat samjho.
            - News aur risk analysis ke bina final decision mat do.
            - Is report se automatic trade execute mat karo.
        """.trimIndent()
    }
}
