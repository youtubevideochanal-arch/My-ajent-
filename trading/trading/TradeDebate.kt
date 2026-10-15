package com.example.myagent

import java.time.Instant

/**
 * MY AGENT - Trade Debate
 *
 * Responsibilities:
 * 1. Bull aur Bear reports compare karna.
 * 2. OHLC candle data validate karna.
 * 3. RSI, MACD, SMA, EMA aur ATR ki basic validation.
 * 4. Historical candle sequence inspect karna.
 * 5. Missing, stale ya invalid data par warning dena.
 *
 * Yeh class live data khud download nahi karti.
 * Yeh broker ko order nahi bhejti.
 */
class TradeDebate {

    data class Indicators(
        val rsi: Double? = null,
        val macd: Double? = null,
        val signalLine: Double? = null,
        val sma20: Double? = null,
        val sma50: Double? = null,
        val ema20: Double? = null,
        val atr: Double? = null,
        val volume: Double? = null
    )

    data class Candle(
        val timestamp: String,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double,
        val volume: Double
    )

    data class SideOpinion(
        val agentName: String,
        val conclusion: String,
        val reasons: List<String>,
        val risks: List<String>,
        val confidence: Double
    )

    data class DebateReport(
        val symbol: String,
        val buySide: SideOpinion,
        val noTradeSide: SideOpinion,
        val indicatorNotes: List<String>,
        val patternNotes: List<String>,
        val dataWarnings: List<String>,
        val finalStatus: String,
        val marketDataTime: String
    )

    private fun isFinitePositive(value: Double?): Boolean {
        return value != null && value.isFinite() && value > 0.0
    }

    private fun isValidCandle(candle: Candle): Boolean {
        return candle.timestamp.isNotBlank() &&
            isFinitePositive(candle.open) &&
            isFinitePositive(candle.high) &&
            isFinitePositive(candle.low) &&
            isFinitePositive(candle.close) &&
            candle.volume.isFinite() &&
            candle.volume >= 0.0 &&
            candle.high >= candle.low &&
            candle.high >= candle.open &&
            candle.high >= candle.close &&
            candle.low <= candle.open &&
            candle.low <= candle.close
    }

    /**
     * Data compare karta hai; BUY ya SELL order nahi banata.
     */
    fun debate(
        symbol: String,
        candles: List<Candle>,
        indicators: Indicators,
        buySide: SideOpinion,
        noTradeSide: SideOpinion,
        marketDataTime: String,
        isLiveData: Boolean = false
    ): DebateReport {

        val warnings = mutableListOf<String>()
        val indicatorNotes = mutableListOf<String>()
        val patternNotes = mutableListOf<String>()

        if (symbol.isBlank()) {
            warnings.add("Symbol missing hai.")
        }

        if (marketDataTime.isBlank()) {
            warnings.add("Market data timestamp missing hai.")
        }

        if (!isLiveData) {
            warnings.add(
                "Data ko live verify nahi kiya gaya. " +
                    "Is report ko live-market analysis na samjhein."
            )
        }

        if (candles.size < 50) {
            warnings.add(
                "50 se kam candles hain; historical analysis limited hai."
            )
        }

        val invalidCount = candles.count { !isValidCandle(it) }

        if (invalidCount > 0) {
            warnings.add(
                "$invalidCount candles invalid hain aur analysis se hata di gayi hain."
            )
        }

        val validCandles = candles
            .filter { isValidCandle(it) }
            .sortedBy { it.timestamp }
            .distinctBy { it.timestamp }

        if (validCandles.size != candles.size - invalidCount) {
            warnings.add(
                "Duplicate timestamps mile; duplicate candles hata di gayi hain."
            )
        }

        if (validCandles.isEmpty()) {
            warnings.add("Koi valid candle available nahi hai.")
        }

        // RSI validation
        val rsi = indicators.rsi

        when {
            rsi == null ->
                indicatorNotes.add("RSI available nahi hai.")

            !rsi.isFinite() || rsi !in 0.0..100.0 ->
                warnings.add("RSI value invalid hai.")

            rsi >= 70.0 ->
                indicatorNotes.add(
                    "RSI 70 ya upar hai; pullback ka risk ho sakta hai."
                )

            rsi <= 30.0 ->
                indicatorNotes.add(
                    "RSI 30 ya neeche hai; reversal ki guarantee nahi."
                )

            else ->
                indicatorNotes.add("RSI 30–70 range mein hai.")
        }

        // MACD validation
        val macd = indicators.macd
        val signal = indicators.signalLine

        if (macd == null || signal == null) {
            indicatorNotes.add("MACD ya signal line missing hai.")
        } else if (!macd.isFinite() || !signal.isFinite()) {
            warnings.add("MACD values invalid hain.")
        } else {
            when {
                macd > signal ->
                    indicatorNotes.add(
                        "MACD signal line ke upar hai."
                    )

                macd < signal ->
                    indicatorNotes.add(
                        "MACD signal line ke neeche hai."
                    )

                else ->
                    indicatorNotes.add(
                        "MACD aur signal line barabar hain."
                    )
            }
        }

        // SMA validation
        val sma20 = indicators.sma20
        val sma50 = indicators.sma50

        if (sma20 != null && !isFinitePositive(sma20)) {
            warnings.add("SMA20 invalid hai.")
        }

        if (sma50 != null && !isFinitePositive(sma50)) {
            warnings.add("SMA50 invalid hai.")
        }

        if (isFinitePositive(sma20) && isFinitePositive(sma50)) {
            when {
                sma20!! > sma50!! ->
                    indicatorNotes.add("SMA20, SMA50 se upar hai.")

                sma20 < sma50 ->
                    indicatorNotes.add("SMA20, SMA50 se neeche hai.")

                else ->
                    indicatorNotes.add("SMA20 aur SMA50 barabar hain.")
            }
        } else {
            indicatorNotes.add("SMA20/SMA50 ka data incomplete hai.")
        }

        // EMA validation
        if (indicators.ema20 != null) {
            if (!isFinitePositive(indicators.ema20)) {
                warnings.add("EMA20 invalid hai.")
            } else {
                indicatorNotes.add("EMA20 value available hai.")
            }
        } else {
            indicatorNotes.add("EMA20 available nahi hai.")
        }

        // ATR validation
        if (indicators.atr != null) {
            if (!indicators.atr.isFinite() || indicators.atr < 0.0) {
                warnings.add("ATR value invalid hai.")
            } else {
                indicatorNotes.add(
                    "ATR volatility measure available hai; " +
                        "yeh direction predict nahi karta."
                )
            }
        } else {
            indicatorNotes.add("ATR available nahi hai.")
        }

        // Volume validation
        if (indicators.volume != null) {
            if (!indicators.volume.isFinite() ||
                indicators.volume < 0.0
            ) {
                warnings.add("Indicator volume invalid hai.")
            } else {
                indicatorNotes.add("Volume value available hai.")
            }
        } else {
            indicatorNotes.add("Indicator volume missing hai.")
        }

        // Historical closing-price sequence
        if (validCandles.size >= 3) {
            val recent = validCandles.takeLast(3)

            val rising = recent.zipWithNext().all { pair ->
                pair.second.close > pair.first.close
            }

            val falling = recent.zipWithNext().all { pair ->
                pair.second.close < pair.first.close
            }

            when {
                rising ->
                    patternNotes.add(
                        "Aakhri 3 valid closes badhe hain. " +
                            "Yeh sirf short sequence hai, confirmed pattern nahi."
                    )

                falling ->
                    patternNotes.add(
                        "Aakhri 3 valid closes gire hain. " +
                            "Yeh sirf short sequence hai, confirmed pattern nahi."
                    )

                else ->
                    patternNotes.add(
                        "Aakhri 3 closes ek hi direction mein nahi hain."
                    )
            }
        } else {
            patternNotes.add(
                "Historical pattern check ke liye valid candles kam hain."
            )
        }

        // Agent opinion validation
        if (buySide.confidence !in 0.0..1.0 ||
            !buySide.confidence.isFinite()
        ) {
            warnings.add("BUY-side confidence 0 se 1 ke beech honi chahiye.")
        }

        if (noTradeSide.confidence !in 0.0..1.0 ||
            !noTradeSide.confidence.isFinite()
        ) {
            warnings.add("NO-TRADE confidence 0 se 1 ke beech honi chahiye.")
        }

        if (buySide.reasons.isEmpty()) {
            warnings.add("BUY-side ke reasons missing hain.")
        }

        if (noTradeSide.reasons.isEmpty()) {
            warnings.add("NO-TRADE side ke reasons missing hain.")
        }

        if (buySide.risks.isEmpty()) {
            warnings.add("BUY-side ne koi risk report nahi kiya.")
        }

        if (noTradeSide.risks.isEmpty()) {
            warnings.add("NO-TRADE side ne koi risk report nahi kiya.")
        }

        /*
         * Conservative result:
         * Yeh module sirf report readiness batata hai.
         * Confidence score ko trade probability nahi maana jayega.
         */
        val finalStatus = when {
            validCandles.isEmpty() ->
                "NO_VALID_MARKET_DATA"

            warnings.isNotEmpty() ->
                "REVIEW_REQUIRED"

            else ->
                "DEBATE_READY"
        }

        return DebateReport(
            symbol = symbol.trim().uppercase(),
            buySide = buySide,
            noTradeSide = noTradeSide,
            indicatorNotes = indicatorNotes,
            patternNotes = patternNotes,
            dataWarnings = warnings,
            finalStatus = finalStatus,
            marketDataTime = marketDataTime
        )
    }

    fun makeReport(report: DebateReport): String {

        fun formatOpinion(opinion: SideOpinion): String {
            return """
                Agent: ${opinion.agentName}
                Conclusion: ${opinion.conclusion}
                Confidence: ${opinion.confidence}

                Reasons:
                ${opinion.reasons.joinToString("\n") { "- $it" }}

                Risks:
                ${opinion.risks.joinToString("\n") { "- $it" }}
            """.trimIndent()
        }

        val warningText = if (report.dataWarnings.isEmpty()) {
            "Koi basic validation warning nahi mili."
        } else {
            report.dataWarnings.joinToString("\n") { "- $it" }
        }

        return """
            MY AGENT - TRADE DEBATE REPORT

            Symbol: ${report.symbol}
            Status: ${report.finalStatus}
            Market data time: ${report.marketDataTime}

            ===== BUY-SIDE ANALYSIS =====
            ${formatOpinion(report.buySide)}

            ===== NO-TRADE ANALYSIS =====
            ${formatOpinion(report.noTradeSide)}

            ===== INDICATORS =====
            ${report.indicatorNotes.joinToString("\n") { "- $it" }}

            ===== HISTORICAL PATTERN NOTES =====
            ${report.patternNotes.joinToString("\n") { "- $it" }}

            ===== DATA WARNINGS =====
            $warningText

            IMPORTANT:
            - Report ke indicators independently calculate aur verify karein.
            - Data timestamp hona live data hone ka proof nahi hai.
            - Historical patterns future movement guarantee nahi karte.
            - Yeh module trade execute nahi karta.
        """.trimIndent()
    }
}
