package com.example.myagent

/**
 * MY AGENT - Bear Agent
 *
 * Kaam:
 * 1. Trade ke against evidence khojna.
 * 2. Downtrend aur weak momentum ko identify karna.
 * 3. Bull Agent ke arguments ko challenge karna.
 * 4. Missing data aur risk report karna.
 *
 * Yeh module trade execute nahi karta.
 */
class BearAgent {

    data class BearReport(
        val symbol: String,
        val riskScore: Int,
        val counterArguments: List<String>,
        val risks: List<String>,
        val missingData: List<String>,
        val conclusion: String
    )

    fun analyze(
        symbol: String,
        chart: ChartAnalysis.ChartReport,
        bullArguments: List<String> = emptyList(),
        news: List<String> = emptyList(),
        researchEvidence: List<String> = emptyList()
    ): BearReport {

        val counterArguments = mutableListOf<String>()
        val risks = mutableListOf<String>()
        val missing = mutableListOf<String>()

        var score = 0

        // 1. Trend ki jaanch
        when (chart.trend) {
            "DOWNTREND" -> {
                risks.add(
                    "SMA20, SMA50 se neeche hai; downtrend ka sanket hai."
                )
                score += 2
            }

            "UPTREND" -> {
                counterArguments.add(
                    "Chart uptrend dikha raha hai, " +
                        "lekin trend reversal ka risk phir bhi rehta hai."
                )
            }

            else -> {
                missing.add(
                    "Trend clear nahi hai; paryapt chart data chahiye."
                )
            }
        }

        // 2. RSI ki jaanch
        val rsi = chart.rsi14

        if (rsi == null) {
            missing.add("RSI data available nahi hai.")
        } else {
            when {
                rsi < 30.0 -> {
                    risks.add(
                        "RSI 30 se neeche hai. Selling pressure ho sakta hai, " +
                            "lekin rebound bhi mumkin hai."
                    )
                    score++
                }

                rsi > 70.0 -> {
                    risks.add(
                        "RSI 70 se upar hai; price overbought ho sakta hai."
                    )
                    score++
                }

                rsi < 45.0 -> {
                    risks.add(
                        "RSI weak momentum ka sanket de sakta hai."
                    )
                    score++
                }

                else -> {
                    counterArguments.add(
                        "RSI akela strong bearish signal confirm nahi karta."
                    )
                }
            }
        }

        // 3. Current close aur SMA20
        val close = chart.lastClose
        val sma20 = chart.sma20

        if (close == null || sma20 == null) {
            missing.add(
                "Latest close ya SMA20 missing hai."
            )
        } else if (close < sma20) {
            risks.add(
                "Latest close SMA20 se neeche hai."
            )
            score++
        } else {
            counterArguments.add(
                "Latest close SMA20 se upar hai; " +
                    "yeh ek possible positive signal hai."
            )
        }

        // 4. Bull Agent ke arguments ko challenge karna
        if (bullArguments.isEmpty()) {
            missing.add(
                "Bull Agent ke arguments nahi mile."
            )
        } else {
            counterArguments.add(
                "Bull Agent ke ${bullArguments.size} argument(s) ko " +
                    "independent evidence se verify karna zaroori hai."
            )
        }

        // 5. News evidence
        if (news.isEmpty()) {
            missing.add(
                "News data nahi diya gaya; news-related risk assess nahi ho sakta."
            )
        } else {
            risks.add(
                "${news.size} news item(s) diye gaye hain; " +
                    "source, date aur market impact verify karna baaki hai."
            )
        }

        // 6. Research evidence
        if (researchEvidence.isEmpty()) {
            missing.add(
                "Independent market research evidence missing hai."
            )
        } else {
            counterArguments.add(
                "${researchEvidence.size} research item(s) review ke liye available hain."
            )
        }

        // 7. Chart warnings
        if (chart.warnings.isNotEmpty()) {
            risks.add(
                "Chart analysis ne data quality ya indicator warnings di hain."
            )
        }

        score = score.coerceIn(0, 5)

        val conclusion = when {
            missing.isNotEmpty() ->
                "INCOMPLETE: Risk assessment ke liye aur verified data chahiye."

            score >= 4 ->
                "HIGHER RISK SIGNALS: Trade se pehle independent risk review zaroori hai."

            score >= 2 ->
                "MIXED OR ELEVATED RISK: Signals ko aur verify karna hoga."

            else ->
                "STRONG BEARISH EVIDENCE NAHI MILA: Iska matlab trade safe hona nahi hai."
        }

        return BearReport(
            symbol = symbol.trim().uppercase(),
            riskScore = score,
            counterArguments = counterArguments,
            risks = risks,
            missingData = missing,
            conclusion = conclusion
        )
    }

    fun toText(report: BearReport): String {

        return """
            MY AGENT - BEAR AGENT REPORT

            Symbol: ${report.symbol}
            Risk score: ${report.riskScore}/5

            Counter arguments:
            ${report.counterArguments.joinToString("\n") { "- $it" }}

            Risks:
            ${report.risks.joinToString("\n") { "- $it" }}

            Missing data:
            ${report.missingData.joinToString("\n") { "- $it" }}

            Conclusion:
            ${report.conclusion}

            Important:
            Risk score probability nahi hai.
            Yeh report akeli trade decision nahi leti.
            Koi bhi signal profit ki guarantee nahi deta.
        """.trimIndent()
    }
}
