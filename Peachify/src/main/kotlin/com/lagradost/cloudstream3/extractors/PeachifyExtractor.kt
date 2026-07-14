package com.lagradost.cloudstream3.extractors

import com.lagradost.cloudstream3.ExtractorApi
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import java.net.URLDecoder

class Peachify : ExtractorApi() {
    override val name = "Peachify"
    override val mainUrl = "https://peachify.top"
    override val requiresReferer = true

    private val peachifyServers = listOf(
        "https://uwu.eat-peach.sbs",
        "https://usa.eat-peach.sbs",
        "https://neon.peachify.top"
    )

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            // Extract media ID and type from URL
            val mediaId = extractMediaId(url)
            val isMovie = url.contains("/movie/")
            val isSeries = url.contains("/tv/") || url.contains("/series/")

            if (mediaId.isEmpty()) return

            // Try multiple servers for redundancy
            peachifyServers.apmap { server ->
                try {
                    val apiUrl = when {
                        isMovie -> "$server/moviebox/movie/$mediaId"
                        isSeries -> {
                            val parts = url.split("/")
                            val season = parts.getOrNull(parts.lastIndex - 1) ?: "1"
                            val episode = parts.lastOrNull() ?: "1"
                            "$server/holly/tv/$mediaId/$season/$episode"
                        }
                        else -> return@apmap
                    }

                    fetchAndParseStreams(
                        apiUrl,
                        callback,
                        subtitleCallback
                    )
                } catch (e: Exception) {
                    println("Error fetching from $server: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Peachify extraction error: ${e.message}")
        }
    }

    private suspend fun fetchAndParseStreams(
        apiUrl: String,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        try {
            val headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "Accept" to "application/json, text/javascript, */*; q=0.01",
                "X-Requested-With" to "XMLHttpRequest",
                "Referer" to "https://peachify.top/"
            )

            val response = app.get(apiUrl, headers = headers)
            val responseText = response.text

            if (responseText.isEmpty() || !responseText.contains("source", ignoreCase = true)) {
                return
            }

            parseEncryptedResponse(responseText, callback, subtitleCallback)
        } catch (e: Exception) {
            println("Error parsing streams: ${e.message}")
        }
    }

    private fun parseEncryptedResponse(
        responseText: String,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        try {
            // Parse sources
            val sourcesPattern = \""(?:src|url)\\"?:\\"?([^\\"\\s,}]+)\"\"\""\n                .toRegex()
            val qualityPattern = \""(?:quality|height|resolution)\\"?:\\"?(\\d+)\"\"\""\n                .toRegex()
            val typePattern = \""(?:type|format)\\"?:\\"?([^\\"\\s,}]+)\"\"\""\n                .toRegex()
            val dubPattern = \""(?:dub|audio|language)\\"?:\\"?([^\\"\\s,}]+)\"\"\""\n                .toRegex()

            val sourcesMatches = sourcesPattern.findAll(responseText).map { it.groupValues[1] }.toList()
            val qualities = qualityPattern.findAll(responseText).map { it.groupValues[1].toIntOrNull() ?: 0 }.toList()
            val types = typePattern.findAll(responseText).map { it.groupValues[1] }.toList()
            val dubs = dubPattern.findAll(responseText).map { it.groupValues[1] }.toList()

            sourcesMatches.forEachIndexed { index, sourceUrl ->
                if (sourceUrl.isNotEmpty() && sourceUrl.startsWith("http")) {
                    val quality = qualities.getOrNull(index) ?: 0
                    val type = types.getOrNull(index) ?: "mp4"
                    val dub = dubs.getOrNull(index) ?: "Original"

                    val streamType = when {
                        type.lowercase().contains("hls") || sourceUrl.contains(".m3u8") -> ExtractorLinkType.HLS
                        type.lowercase().contains("dash") || sourceUrl.contains(".mpd") -> ExtractorLinkType.DASH
                        else -> ExtractorLinkType.VIDEO
                    }

                    callback(
                        ExtractorLink(
                            source = "Peachify",
                            name = "Peachify - $dub - ${quality}p",
                            url = URLDecoder.decode(sourceUrl, "UTF-8"),
                            referer = "https://peachify.top/",
                            quality = mapQuality(quality),
                            type = streamType,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0",
                                "Referer" to "https://peachify.top/"
                            )
                        )
                    )
                }
            }

            // Parse subtitles
            val subtitlePattern = \""\\{[^}]*(?:subtitle|subs|caption)[^}]*\\}\"\"\""\n                .toRegex()
            subtitlePattern.findAll(responseText).forEach { match ->
                val subtitleBlock = match.value
                val urlPattern = \""(?:url|src)\\"?:\\"?([^\\"\\s,}]+)\"\"\""\n                    .toRegex()
                val langPattern = \""(?:lang|language|label)\\"?:\\"?([^\\"\\s,}]+)\"\"\""\n                    .toRegex()

                val subUrl = urlPattern.find(subtitleBlock)?.groupValues?.get(1)
                val lang = langPattern.find(subtitleBlock)?.groupValues?.get(1)

                if (!subUrl.isNullOrEmpty() && !lang.isNullOrEmpty()) {
                    subtitleCallback(
                        SubtitleFile(
                            lang,
                            URLDecoder.decode(subUrl, "UTF-8")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("Error parsing response: ${e.message}")
        }
    }

    private fun mapQuality(quality: Int): Int {
        return when {
            quality >= 1080 -> Qualities.P1080.value
            quality >= 720 -> Qualities.P720.value
            quality >= 480 -> Qualities.P480.value
            quality >= 360 -> Qualities.P360.value
            else -> Qualities.Unknown.value
        }
    }

    private fun extractMediaId(url: String): String {
        return try {
            val parts = url.split("/").filter { it.isNotEmpty() }
            parts.lastOrNull() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
