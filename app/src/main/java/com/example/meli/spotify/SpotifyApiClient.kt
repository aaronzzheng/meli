package com.example.meli.spotify

import android.util.Log
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "SpotifyApiClient"

class SpotifyApiException(
    val statusCode: Int,
    val responseBody: String,
    message: String
) : IOException(message)

class SpotifyApiClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    fun exchangeCodeForTokens(code: String, codeVerifier: String): SpotifyTokens {
        val body = FormBody.Builder()
            .add("client_id", SpotifyConfig.clientId)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", SpotifyConfig.redirectUri)
            .add("code_verifier", codeVerifier)
            .build()
        return executeTokenRequest(body)
    }

    fun refreshAccessToken(refreshToken: String): SpotifyTokens {
        val body = FormBody.Builder()
            .add("client_id", SpotifyConfig.clientId)
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()
        return executeTokenRequest(body)
    }

    fun fetchCurrentUserProfile(accessToken: String): SpotifyProfile {
        val json = executeApiGet("/me", accessToken)
        return SpotifyJsonParser.parseProfile(json)
    }

    fun fetchPlaylists(accessToken: String): List<SpotifyPlaylist> {
        val json = executeApiGet("/me/playlists?limit=20", accessToken)
        return SpotifyJsonParser.parsePlaylists(json)
    }

    fun fetchRecentlyPlayed(accessToken: String): List<SpotifyTrack> {
        val json = executeApiGet("/me/player/recently-played?limit=20", accessToken)
        return SpotifyJsonParser.parseRecentlyPlayed(json)
    }

    fun fetchTopTracks(accessToken: String): List<SpotifyTrack> {
        val json = executeApiGet("/me/top/tracks?limit=20&time_range=medium_term", accessToken)
        return SpotifyJsonParser.parseTracksFromItems(json.optJSONArray("items") ?: JSONArray())
    }

    fun fetchTopArtists(accessToken: String): List<SpotifyArtist> {
        val json = executeApiGet("/me/top/artists?limit=20&time_range=medium_term", accessToken)
        return SpotifyJsonParser.parseArtists(json.optJSONArray("items") ?: JSONArray())
    }

    fun fetchSavedTracks(accessToken: String): List<SpotifyTrack> {
        val json = executeApiGet("/me/tracks?limit=20", accessToken)
        return SpotifyJsonParser.parseSavedTracks(json)
    }

    fun searchTracks(accessToken: String, query: String): List<SpotifyTrack> {
        val sanitizedQuery = query
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace("\n", " ")

        val candidates = listOf(
            sanitizedQuery,
            "\"$sanitizedQuery\"",
            sanitizedQuery.replace(":", " ").replace("/", " ")
        ).distinct()

        var lastError: SpotifyApiException? = null
        for (candidate in candidates) {
            try {
                val json = executeTrackSearch(accessToken, candidate, includeMarket = true)
                val items = json.optJSONObject("tracks")?.optJSONArray("items") ?: JSONArray()
                return SpotifyJsonParser.parseTracksFromItems(items)
            } catch (error: SpotifyApiException) {
                lastError = error
                Log.e(TAG, "Track search failed with market for query='$candidate': ${error.responseBody}")
                if (error.statusCode != 400) throw error
            }

            try {
                val json = executeTrackSearch(accessToken, candidate, includeMarket = false)
                val items = json.optJSONObject("tracks")?.optJSONArray("items") ?: JSONArray()
                return SpotifyJsonParser.parseTracksFromItems(items)
            } catch (error: SpotifyApiException) {
                lastError = error
                Log.e(TAG, "Track search failed without market for query='$candidate': ${error.responseBody}")
                if (error.statusCode != 400) throw error
            }
        }

        throw lastError ?: IllegalStateException("Spotify track search failed")
    }

    private fun executeTrackSearch(
        accessToken: String,
        query: String,
        includeMarket: Boolean
    ): JSONObject {
        val urlBuilder = "${SpotifyConfig.apiBaseUrl}/search".toHttpUrl()
            .newBuilder()
            .addQueryParameter("type", "track")
            .addQueryParameter("limit", "10")
            .addQueryParameter("q", query)

        if (includeMarket) {
            urlBuilder.addQueryParameter("market", "from_token")
        }

        return executeApiGet(urlBuilder.build().toString(), accessToken, absoluteUrl = true)
    }

    fun fetchPlaylistTracks(accessToken: String, playlistId: String): List<SpotifyTrack> {
        val json = runCatching {
            executeApiGet(
                "/playlists/$playlistId/tracks?market=from_token&limit=50&additional_types=track",
                accessToken
            )
        }.recoverCatching { throwable ->
            if (throwable is SpotifyApiException && throwable.statusCode == 403) {
                executeApiGet("/playlists/$playlistId/tracks?limit=50", accessToken)
            } else {
                throw throwable
            }
        }.getOrThrow()
        return SpotifyJsonParser.parsePlaylistTracks(json)
    }

    fun fetchPlaylistDetails(accessToken: String, playlistId: String): SpotifyPlaylistDetails {
        val json = runCatching {
            executeApiGet(
                "/playlists/$playlistId?market=from_token&fields=id,name,description,owner(display_name),images,tracks(total)",
                accessToken
            )
        }.recoverCatching { throwable ->
            if (throwable is SpotifyApiException && throwable.statusCode == 403) {
                executeApiGet("/playlists/$playlistId", accessToken)
            } else {
                throw throwable
            }
        }.getOrThrow()
        val playlist = SpotifyJsonParser.parsePlaylist(json)
        val tracks = fetchPlaylistTracks(accessToken, playlistId)
        return SpotifyPlaylistDetails(
            playlist = playlist,
            tracks = tracks
        )
    }

    private fun executeTokenRequest(body: FormBody): SpotifyTokens {
        val request = Request.Builder()
            .url(SpotifyConfig.tokenEndpoint)
            .post(body)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e(TAG, "Token request failed: ${response.code} $responseBody")
                throw SpotifyApiException(
                    statusCode = response.code,
                    responseBody = responseBody,
                    message = "Spotify token exchange failed (${response.code})"
                )
            }
            val json = JSONObject(responseBody)
            val expiresIn = json.optLong("expires_in", 0L)
            return SpotifyTokens(
                accessToken = json.getString("access_token"),
                refreshToken = json.optString("refresh_token").ifBlank { null },
                expiresInSeconds = expiresIn,
                expiresAtMillis = System.currentTimeMillis() + (expiresIn * 1000L)
            )
        }
    }

    private fun executeApiGet(path: String, accessToken: String, absoluteUrl: Boolean = false): JSONObject {
        val request = Request.Builder()
            .url(if (absoluteUrl) path else "${SpotifyConfig.apiBaseUrl}$path")
            .get()
            .header("Authorization", "Bearer $accessToken")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e(TAG, "API request failed: ${response.code} $path $responseBody")
                throw SpotifyApiException(
                    statusCode = response.code,
                    responseBody = responseBody,
                    message = "Spotify API request failed (${response.code})"
                )
            }
            return JSONObject(responseBody)
        }
    }

    private object SpotifyJsonParser {
        fun parseProfile(json: JSONObject): SpotifyProfile {
            return SpotifyProfile(
                id = json.optString("id"),
                displayName = json.optString("display_name").ifBlank { json.optString("id") },
                email = json.optString("email").ifBlank { null },
                country = json.optString("country").ifBlank { null },
                product = json.optString("product").ifBlank { null },
                imageUrl = imageUrl(json.optJSONArray("images"))
            )
        }

        fun parsePlaylists(json: JSONObject): List<SpotifyPlaylist> {
            val items = json.optJSONArray("items") ?: JSONArray()
            return buildList {
                for (index in 0 until items.length()) {
                    val playlist = items.optJSONObject(index) ?: continue
                    add(
                        SpotifyPlaylist(
                            id = playlist.optString("id"),
                            name = playlist.optString("name"),
                            description = playlist.optString("description").ifBlank { null },
                            ownerId = playlist.optJSONObject("owner")
                                ?.optString("id")
                                ?.ifBlank { null },
                            ownerName = playlist.optJSONObject("owner")
                                ?.optString("display_name")
                                ?.ifBlank { null },
                            totalTracks = playlist.optJSONObject("tracks")?.optInt("total") ?: 0,
                            imageUrl = imageUrl(playlist.optJSONArray("images"))
                        )
                    )
                }
            }
        }

        fun parseRecentlyPlayed(json: JSONObject): List<SpotifyTrack> {
            val items = json.optJSONArray("items") ?: JSONArray()
            return buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val track = item.optJSONObject("track") ?: continue
                    add(
                        parseTrack(track).copy(
                            playedAt = item.optString("played_at").ifBlank { null }
                        )
                    )
                }
            }
        }

        fun parseSavedTracks(json: JSONObject): List<SpotifyTrack> {
            val items = json.optJSONArray("items") ?: JSONArray()
            return buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val track = item.optJSONObject("track") ?: continue
                    add(
                        parseTrack(track).copy(
                            addedAt = item.optString("added_at").ifBlank { null }
                        )
                    )
                }
            }
        }

        fun parsePlaylistTracks(json: JSONObject): List<SpotifyTrack> {
            val items = json.optJSONArray("items") ?: JSONArray()
            return buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val track = item.optJSONObject("track") ?: continue
                    add(parseTrack(track))
                }
            }
        }

        fun parsePlaylistDetails(json: JSONObject): SpotifyPlaylistDetails {
            return SpotifyPlaylistDetails(
                playlist = parsePlaylist(json),
                tracks = emptyList()
            )
        }

        fun parsePlaylist(json: JSONObject): SpotifyPlaylist {
            return SpotifyPlaylist(
                id = json.optString("id"),
                name = json.optString("name"),
                description = json.optString("description").ifBlank { null },
                ownerId = json.optJSONObject("owner")?.optString("id")?.ifBlank { null },
                ownerName = json.optJSONObject("owner")?.optString("display_name")?.ifBlank { null },
                totalTracks = json.optJSONObject("tracks")?.optInt("total") ?: 0,
                imageUrl = imageUrl(json.optJSONArray("images"))
            )
        }

        fun parseTracksFromItems(items: JSONArray): List<SpotifyTrack> {
            return buildList {
                for (index in 0 until items.length()) {
                    val track = items.optJSONObject(index) ?: continue
                    add(parseTrack(track))
                }
            }
        }

        fun parseArtists(items: JSONArray): List<SpotifyArtist> {
            return buildList {
                for (index in 0 until items.length()) {
                    val artist = items.optJSONObject(index) ?: continue
                    add(
                        SpotifyArtist(
                            id = artist.optString("id"),
                            name = artist.optString("name"),
                            genres = stringList(artist.optJSONArray("genres")),
                            imageUrl = imageUrl(artist.optJSONArray("images"))
                        )
                    )
                }
            }
        }

        private fun parseTrack(track: JSONObject): SpotifyTrack {
            val album = track.optJSONObject("album")
            return SpotifyTrack(
                id = track.optString("id"),
                name = track.optString("name"),
                artistNames = parseArtistNames(track.optJSONArray("artists")),
                albumName = album?.optString("name")?.ifBlank { null },
                imageUrl = album?.optJSONArray("images")?.let(::imageUrl)
            )
        }

        private fun parseArtistNames(items: JSONArray?): List<String> {
            return buildList {
                if (items == null) return@buildList
                for (index in 0 until items.length()) {
                    val artist = items.optJSONObject(index) ?: continue
                    val name = artist.optString("name")
                    if (name.isNotBlank()) {
                        add(name)
                    }
                }
            }
        }

        private fun stringList(items: JSONArray?): List<String> {
            return buildList {
                if (items == null) return@buildList
                for (index in 0 until items.length()) {
                    val value = items.optString(index)
                    if (value.isNotBlank()) {
                        add(value)
                    }
                }
            }
        }

        private fun imageUrl(images: JSONArray?): String? {
            if (images == null || images.length() == 0) return null
            return images.optJSONObject(0)?.optString("url")?.ifBlank { null }
        }
    }
}
