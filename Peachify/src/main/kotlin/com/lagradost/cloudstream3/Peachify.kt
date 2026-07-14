package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.extractors.Peachify as PeachifyExtractor

class Peachify : MainAPI() {
    override var mainUrl = "https://peachify.top"
    override var name = "Peachify"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override val lang = "en"

    private val peachifyServers = listOf(
        "https://uwu.eat-peach.sbs/moviebox",
        "https://usa.eat-peach.sbs/holly",
        "https://usa.eat-peach.sbs/air",
        "https://usa.eat-peach.sbs/multi",
        "https://uwu.eat-peach.sbs/net",
        "https://uwu.eat-peach.sbs/bmb"
    )

    override suspend fun getMainPage(): HomePageResponse {
        val pages = mutableListOf<HomePageList>()

        // Featured/Popular Movies
        pages.add(
            HomePageList(
                "Popular Movies",
                parseMoviesFromMain("https://peachify.top"),
                isHorizontalImages = true
            )
        )

        // Trending TV Series
        pages.add(
            HomePageList(
                "Popular Series",
                parseSeriesFromMain("https://peachify.top"),
                isHorizontalImages = true
            )
        )

        return HomePageResponse(pages)
    }

    private suspend fun parseMoviesFromMain(url: String): List<SearchResponse> {
        val response = app.get(url).document
        val movies = mutableListOf<SearchResponse>()
        
        try {
            response.select(".movie-item").forEach { item ->
                val title = item.selectFirst(".title")?.text() ?: return@forEach
                val href = item.selectFirst("a")?.attr("href") ?: return@forEach
                val posterUrl = item.selectFirst("img")?.attr("src") ?: ""
                val year = item.selectFirst(".year")?.text()?.toIntOrNull()
                val imdbId = extractImdbId(href)

                movies.add(
                    newMovieSearchResponse(
                        title,
                        "$mainUrl/movie/$imdbId",
                        TvType.Movie,
                        posterUrl
                    ) {
                        this.year = year
                    }
                )
            }
        } catch (e: Exception) {
            logError(e)
        }

        return movies
    }

    private suspend fun parseSeriesFromMain(url: String): List<SearchResponse> {
        val response = app.get(url).document
        val series = mutableListOf<SearchResponse>()
        
        try {
            response.select(".series-item").forEach { item ->
                val title = item.selectFirst(".title")?.text() ?: return@forEach
                val href = item.selectFirst("a")?.attr("href") ?: return@forEach
                val posterUrl = item.selectFirst("img")?.attr("src") ?: ""
                val year = item.selectFirst(".year")?.text()?.toIntOrNull()
                val imdbId = extractImdbId(href)

                series.add(
                    newTvSeriesSearchResponse(
                        title,
                        "$mainUrl/series/$imdbId",
                        TvType.TvSeries,
                        posterUrl
                    ) {
                        this.year = year
                    }
                )
            }
        } catch (e: Exception) {
            logError(e)
        }

        return series
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> {
        return search(query)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/search?q=$query"
        val results = mutableListOf<SearchResponse>()

        try {
            val response = app.get(searchUrl).document
            response.select(".search-result").forEach { item ->
                val title = item.selectFirst(".title")?.text() ?: return@forEach
                val href = item.selectFirst("a")?.attr("href") ?: return@forEach
                val posterUrl = item.selectFirst("img")?.attr("src") ?: ""
                val type = if (item.hasClass("movie")) TvType.Movie else TvType.TvSeries
                val year = item.selectFirst(".year")?.text()?.toIntOrNull()

                val searchResponse = when (type) {
                    TvType.Movie -> newMovieSearchResponse(
                        title,
                        href,
                        TvType.Movie,
                        posterUrl
                    ) { this.year = year }
                    else -> newTvSeriesSearchResponse(
                        title,
                        href,
                        TvType.TvSeries,
                        posterUrl
                    ) { this.year = year }
                }
                results.add(searchResponse)
            }
        } catch (e: Exception) {
            logError(e)
        }

        return results
    }

    override suspend fun load(url: String): LoadResponse? {
        val tmdbId = extractTmdbId(url)
        val isMovie = url.contains("/movie/")

        return try {
            if (isMovie) {
                loadMovie(tmdbId, url)
            } else {
                loadSeries(tmdbId, url)
            }
        } catch (e: Exception) {
            logError(e)
            null
        }
    }

    private suspend fun loadMovie(tmdbId: String, url: String): MovieLoadResponse? {
        val doc = app.get(url).document

        val title = doc.selectFirst("h1.title")?.text() ?: return null
        val description = doc.selectFirst(".description")?.text()
        val posterUrl = doc.selectFirst(".poster img")?.attr("src") ?: ""
        val rating = doc.selectFirst(".rating")?.text()?.toFloatOrNull()?.times(10)?.toInt()
        val year = doc.selectFirst(".year")?.text()?.toIntOrNull()
        val duration = doc.selectFirst(".duration")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val genres = doc.select(".genre").map { it.text() }
        val tags = doc.select(".tag").map { it.text() }

        return newMovieLoadResponse(
            title,
            url,
            TvType.Movie,
            url
        ) {
            this.posterUrl = posterUrl
            this.year = year
            this.plot = description
            this.rating = rating
            this.duration = duration
            this.tags = genres + tags
        }
    }

    private suspend fun loadSeries(tmdbId: String, url: String): TvSeriesLoadResponse? {
        val doc = app.get(url).document

        val title = doc.selectFirst("h1.title")?.text() ?: return null
        val description = doc.selectFirst(".description")?.text()
        val posterUrl = doc.selectFirst(".poster img")?.attr("src") ?: ""
        val rating = doc.selectFirst(".rating")?.text()?.toFloatOrNull()?.times(10)?.toInt()
        val year = doc.selectFirst(".year")?.text()?.toIntOrNull()
        val genres = doc.select(".genre").map { it.text() }
        val tags = doc.select(".tag").map { it.text() }

        val episodes = mutableListOf<Episode>()
        doc.select(".season").forEach { season ->
            val seasonNum = season.selectFirst(".season-number")?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 1
            season.select(".episode").forEach { episode ->
                val epNum = episode.selectFirst(".episode-number")?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 1
                val epTitle = episode.selectFirst(".episode-title")?.text() ?: ""
                val epDesc = episode.selectFirst(".episode-description")?.text()
                val epPoster = episode.selectFirst("img")?.attr("src") ?: posterUrl

                episodes.add(
                    Episode(
                        "$url|$seasonNum|$epNum",
                        epTitle,
                        seasonNum,
                        epNum,
                        epDesc,
                        epPoster
                    )
                )
            }
        }

        return newTvSeriesLoadResponse(
            title,
            url,
            TvType.TvSeries,
            episodes
        ) {
            this.posterUrl = posterUrl
            this.year = year
            this.plot = description
            this.rating = rating
            this.tags = genres + tags
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        try {
            val parts = data.split("|")
            val url = parts.getOrNull(0) ?: return false
            val isMovie = url.contains("/movie/")
            val tmdbId = extractTmdbId(url)

            peachifyServers.forEach { server ->
                try {
                    val apiUrl = if (isMovie) {
                        "$server/movie/$tmdbId"
                    } else {
                        val season = parts.getOrNull(1)?.toIntOrNull() ?: 1
                        val episode = parts.getOrNull(2)?.toIntOrNull() ?: 1
                        "$server/tv/$tmdbId/$season/$episode"
                    }

                    val response = app.get(apiUrl, headers = mapOf(
                        "User-Agent" to "Mozilla/5.0",
                        "Accept" to "application/json",
                        "Referer" to "https://peachify.top/"
                    )).text

                    if (response.contains("sources")) {
                        parseSourcesFromResponse(response, callback, subtitleCallback)
                    }
                } catch (e: Exception) {
                    logError(e)
                }
            }

            return true
        } catch (e: Exception) {
            logError(e)
            return false
        }
    }

    private fun parseSourcesFromResponse(
        response: String,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        try {
            val sourcesRegex = "\"url\":\"([^\"]+)\"".toRegex()
            val qualityRegex = "\"quality\":\"([^\"]+)\"".toRegex()
            val typeRegex = "\"type\":\"([^\"]+)\"".toRegex()

            sourcesRegex.findAll(response).forEach { matchResult ->
                val sourceUrl = matchResult.groupValues[1]
                val quality = qualityRegex.find(response)?.groupValues?.get(1) ?: "Unknown"
                val type = typeRegex.find(response)?.groupValues?.get(1) ?: "mp4"

                if (sourceUrl.isNotEmpty()) {
                    callback(
                        ExtractorLink(
                            source = "Peachify",
                            name = "Peachify - $quality",
                            url = sourceUrl,
                            referer = "https://peachify.top/",
                            quality = parseQuality(quality),
                            type = if (type.lowercase().contains("hls")) ExtractorLinkType.HLS else ExtractorLinkType.VIDEO
                        )
                    )
                }
            }

            val subtitleRegex = "\"url\":\"([^\"]+)\",\"label\":\"([^\"]+)\",\"format\":\"vtt\"".toRegex()
            subtitleRegex.findAll(response).forEach { matchResult ->
                val subUrl = matchResult.groupValues[1]
                val language = matchResult.groupValues[2]
                if (subUrl.isNotEmpty()) {
                    subtitleCallback(
                        SubtitleFile(
                            language,
                            subUrl
                        )
                    )
                }
            }
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun parseQuality(qualityStr: String): Int {
        return when {
            qualityStr.contains("1080") -> Qualities.P1080.value
            qualityStr.contains("720") -> Qualities.P720.value
            qualityStr.contains("480") -> Qualities.P480.value
            qualityStr.contains("360") -> Qualities.P360.value
            else -> Qualities.Unknown.value
        }
    }

    private fun extractTmdbId(url: String): String {
        return url.split("/").lastOrNull { it.isNotEmpty() } ?: ""
    }

    private fun extractImdbId(url: String): String {
        return url.split("/").lastOrNull { it.isNotEmpty() } ?: ""
    }
}
