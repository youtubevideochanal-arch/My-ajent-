package com.example.myagent

/**
 * MY AGENT - Bull Agent
 *
 * Kaam:
 * Market mein price upar jaane ke paksh mein
 * available evidence ko evaluate karna.
 *
 * Yeh module trade execute nahi karta.
 */
class BullAgent {

    data class BullReport(
        val symbol: String,
        val score: Int,
        val arguments: List<String>,
        val risks: List<String>,
        val missingData: List<String>,
        val conclusion: String
    )

    fun analyze(
        symbol: String,
        chart: ChartAnalysis.ChartReport,
        news: List<String> = emptyList(),
        researchEvidence: List<String> = emptyList()
    ): BullReport {

        val arguments = mutableListOf<String>()
        val risks = mutableListOf<String>()
        val missing = mutableListOf<String>()

        var score = 0

        // Chart trend
        when (chart.trend) {
            "UPTREND" -> {
                arguments.add(
                    "Chart report mein SMA20, SMA50 se upar hai."
                )
                score += 2
            }

            "DOWNTREND" -> {
                risks.add(
                    "Chart report mein SMA20, SMA50 se neeche hai."
                )
            }

            else -> {
                missing.add(
                    "Trend confirm karne ke liye paryapt data nahi hai."
                )
            }
        }

        // RSI
        val rsi = chart.rsi14

        if (rsi == null) {
            missing.add("RSI data available nahi hai.")
        } else {
            when {
                rsi in 50.0..65.0 -> {
                    arguments.add(
                        "RSI moderate positive momentum dikha raha hai."
                    )
                    score++
                }

                rsi > 70.0 -> {
                    risks.add(
                        "RSI 70 se upar hai; price overextended ho sakta hai."
                    )
                }

                rsi < 30.0 -> {
                    arguments.add(
                        "RSI low hai; possible recovery ko aur evidence se check karein."
                    )
                }

                else -> {
                    arguments.add(
                        "RSI mein strong bullish confirmation nahi hai."
                    )
                }
            }
        }

        // Price aur moving average
        val close = chart.lastClose
        val sma20 = chart.sma20

        if (close == null || sma20 == null) {
            missing.add(
                "Latest close ya SMA20 missing hai."
            )
        } else if (close > sma20) {
            arguments.add(
                "Latest close SMA20 se upar hai."
            )
            score++
        } else {
            risks.add(
                "Latest close SMA20 se neeche ya uske barabar hai."
            )
        }

        // News: sirf diye gaye text ko evidence maana jayega.
        if (news.isEmpty()) {
            missing.add(
                "News evidence nahi diya gaya; sentiment confirm nahi hai."
            )
        } else {
            arguments.add(
                "${news.size} news item(s) review ke liye diye gaye hain."
            )
            risks.add(
                "News ka sentiment aur sachchai independently verify karni hogi."
            )
        }

        // Research evidence
        if (researchEvidence.isEmpty()) {
            missing.add(
                "Independent market research evidence missing hai."
            )
        } else {
            arguments.add(
                "${researchEvidence.size} research item(s) available hain."
            )
        }

        if (chart.warnings.isNotEmpty()) {
            risks.add(
                "Chart module ne data quality ya indicator warnings di hain."
            )
        }

        score = score.coerceIn(0, 5)

        val conclusion = when {
            missing.isNotEmpty() ->
                "INCOMPLETE: Bullish case ke liye aur verified data chahiye."

            score >= 4 ->
                "POSSIBLE BULLISH CASE: BearAgent aur risk checks zaroori hain."

            score >= 2 ->
                "WEAK BULLISH CASE: Signals mixed hain."

            else ->
                "BULLISH CASE WEAK: Mazboot positive evidence nahi mila."
        }

        return BullReport(
            symbol = symbol.trim().uppercase(),
            score = score,
            arguments = arguments,
            risks = risks,
            missingData = missing,
            conclusion = conclusion
        )
    }

    fun toText(report: BullReport): String {
        return """
            MY AGENT - BULL AGENT REPORT

            Symbol: ${report.symbol}
            Evidence score: ${report.score}/5

            Positive arguments:
            ${report.arguments.joinToString("\n") { "- $it" }}

            Risks:
            ${report.risks.joinToString("\n") { "- $it" }}

            Missing data:
            ${report.missingData.joinToString("\n") { "- $it" }}

            Conclusion:
            ${report.conclusion}

            Note:
            Yeh sirf ek analysis module hai.
            Iska score probability ya guaranteed profit nahi hai.
            Yeh report akeli trade lene ka signal nahi hai.
        """.trimIndent()
    }
}
