package com.example.myagent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

/**
 * MY AGENT - News Feed
 *
 * Kaam:
 * 1. Public RSS feeds se news collect karna.
 * 2. Headline, link aur publication time nikalna.
 * 3. Duplicate headlines hatana.
 * 4. NewsResearch module ko clean news data dena.
 *
 * Yeh module trade ka decision nahi lega.
 */
class NewsFeed {

    data class Article(
        val title: String,
        val link: String,
        val publishedAt: String,
        val source: String
    )

    data class FeedResult(
        val articles: List<Article>,
        val warnings: List<String>
    )

    /**
     * Public RSS feed download karke articles parse karta hai.
     *
     * Dhyan dein:
     * Har website RSS access allow nahi karti.
     * Internet aur valid RSS URL zaroori hain.
     */
    suspend fun fetchFeed(
        feedUrl: String,
        sourceName: String
    ): FeedResult = withContext(Dispatchers.IO) {

        val warnings = mutableListOf<String>()

        try {
            val url = URL(feedUrl)

            require(
                url.protocol.equals("https", ignoreCase = true)
            ) {
                "Sirf HTTPS RSS feeds allowed hain."
            }

            val connection =
                (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 15000

                    setRequestProperty(
                        "User-Agent",
                        "MY-AGENT-NewsReader/1.0"
                    )

                    setRequestProperty(
                        "Accept",
                        "application/rss+xml, application/xml, text/xml"
                    )
                }

            try {
                val status = connection.responseCode

                if (status !in 200..299) {
                    warnings.add(
                        "$sourceName RSS ne HTTP $status response diya."
                    )

                    return@withContext FeedResult(
                        articles = emptyList(),
                        warnings = warnings
                    )
                }

                val factory =
                    DocumentBuilderFactory.newInstance()

                // XML external entities ko disable karna.
                factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
                )

                factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false
                )

                factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false
                )

                factory.isXIncludeAware = false
                factory.isExpandEntityReferences = false

                val document = connection.inputStream
                    .buffered()
                    .use { input ->
                        factory.newDocumentBuilder()
                            .parse(input)
                    }

                val nodes = document.getElementsByTagName("item")
                val articles = mutableListOf<Article>()

                for (index in 0 until nodes.length) {
                    val element = nodes.item(index) as? Element
                        ?: continue

                    fun text(tag: String): String {
                        return element
                            .getElementsByTagName(tag)
                            .item(0)
                            ?.textContent
                            ?.trim()
                            .orEmpty()
                    }

                    val title = text("title")
                    val link = text("link")
                    val publishedAt = text("pubDate")

                    if (title.isNotBlank() && link.isNotBlank()) {
                        articles.add(
                            Article(
                                title = title,
                                link = link,
                                publishedAt = publishedAt,
                                source = sourceName
                            )
                        )
                    }
                }

                val uniqueArticles = articles.distinctBy {
                    it.link.trim().lowercase()
                }

                if (uniqueArticles.isEmpty()) {
                    warnings.add(
                        "RSS feed se koi valid article nahi mila."
                    )
                }

                FeedResult(
                    articles = uniqueArticles,
                    warnings = warnings
                )
            } finally {
                connection.disconnect()
            }

        } catch (e: Exception) {
            warnings.add(
                "$sourceName feed read nahi ho saki: " +
                    (e.message ?: "Unknown error")
            )

            FeedResult(
                articles = emptyList(),
                warnings = warnings
            )
        }
    }

    /**
     * Kai RSS feeds ko collect karke ek report banata hai.
     */
    suspend fun collect(
        feeds: List<Pair<String, String>>
    ): FeedResult = withContext(Dispatchers.IO) {

        val allArticles = mutableListOf<Article>()
        val allWarnings = mutableListOf<String>()

        for ((sourceName, feedUrl) in feeds) {
            val result = fetchFeed(
                feedUrl = feedUrl,
                sourceName = sourceName
            )

            allArticles.addAll(result.articles)
            allWarnings.addAll(result.warnings)
        }

        val uniqueArticles = allArticles.distinctBy {
            it.link.trim().lowercase()
        }

        FeedResult(
            articles = uniqueArticles,
            warnings = allWarnings.distinct()
        )
    }

    /**
     * Collected news ko agle analysis module ke liye text mein badalta hai.
     */
    fun makeResearchInput(
        result: FeedResult
    ): String {

        val newsText = result.articles.joinToString(
            separator = "\n\n"
        ) { article ->
            """
            Headline: ${article.title}
            Source: ${article.source}
            Published: ${article.publishedAt.ifBlank { "Unknown" }}
            Link: ${article.link}
            """.trimIndent()
        }

        val warningText = if (result.warnings.isEmpty()) {
            "Koi feed error report nahi hua."
        } else {
            result.warnings.joinToString("\n") { "- $it" }
        }

        return """
            MY AGENT - NEWS FEED REPORT

            Articles:
            $newsText

            Warnings:
            $warningText

            Rules:
            - Headlines ko independently confirmed facts mat samjho.
            - Article ka original link preserve karo.
            - Purani news ko latest news mat batao.
            - Sirf headlines ke basis par trade decide mat karo.
            - Yeh report NewsResearch module ko di ja sakti hai.
        """.trimIndent()
    }
}
