package dev.daika.davy.data.repository

import android.content.Context
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.daika.davy.data.api.YummyApi
import dev.daika.davy.data.model.AnimeGenreFilterDto
import dev.daika.davy.data.model.toEntity
import dev.daika.davy.domain.entity.Anime
import dev.daika.davy.domain.entity.AnimeGenreFilter
import dev.daika.davy.domain.entity.Feed
import dev.daika.davy.utils.ExpiringCache
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import dev.daika.davy.domain.entity.AnimeFilterState
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.InputStream
import kotlin.io.path.div

private const val TAG = "YummyRepository"
private const val GENRES_CACHE_PREFS = "yummy_cache"
private const val GENRES_CACHE_KEY = "all"
private const val GENRES_CACHE_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000

@Singleton
class YummyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val yummyApi: YummyApi
) {
    private val animeCache = ExpiringCache<Int, Anime>(5, 3_600_000)
    private val cache by lazy {
        context.cacheDir
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getFeed(): Feed {
        val feed = yummyApi.getFeed().toEntity()
        return feed

    }

    suspend fun getAnimeDetails(id: Int, needVideos: Boolean): Anime {
        val cachedAnime = animeCache.get(id)
        return if (cachedAnime != null) {
            if (!needVideos || cachedAnime.translations.isNotEmpty()) {
                cachedAnime
            } else {
                val translations = yummyApi.getAnimeVideos(id).toEntity()
                val animeDetails = cachedAnime.copy(translations = translations)
                animeCache.put(id, animeDetails)
                animeDetails
            }
        } else {
            val animeDetails = yummyApi.getAnimeDetails(id, needVideos).toEntity()
            animeCache.put(id, animeDetails)
            animeDetails
        }
    }

    fun searchAnime(query: String, filter: AnimeFilterState) = Pager(
        config = PagingConfig(
            pageSize = 20,
            initialLoadSize = 20,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { YummySearchPagingSource(yummyApi, query, filter) }
    ).flow

    suspend fun getAnimeGenres(): List<AnimeGenreFilter> {
        readGenresFromDisk()?.let { return it }

        val genres = yummyApi.getAnimeGenres().genres.map { it.toEntity() }
        writeGenresToDisk(genres)
        return genres
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun readGenresFromDisk(): List<AnimeGenreFilter>? {
        val cache_file = File(cache, "genres.json")
        if (!cache_file.exists()) return null
        val raw = cache_file.inputStream()
        return try {
            val cached = json.decodeFromStream<GenresDiskCache>(raw)
            if (System.currentTimeMillis() - cached.savedAtMillis > GENRES_CACHE_TTL_MILLIS) {
                cache_file.delete()
                null
            } else {
                cached.genres.map { it.toEntity() }
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Failed to decode genres cache", e)
            cache_file.delete()
            null
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun writeGenresToDisk(genres: List<AnimeGenreFilter>) {
        val payload = GenresDiskCache(
            savedAtMillis = System.currentTimeMillis(),
            genres = genres.map {
                AnimeGenreFilterDto(
                    title = it.title,
                    href = it.id,
                    value = 0,
                    moreTitles = emptyList(),
                    groupId = 0
                )
            }
        )

        json.encodeToStream(payload, File(cache, "genres.json").outputStream())
    }

    @Serializable
    private data class GenresDiskCache(
        val savedAtMillis: Long,
        val genres: List<AnimeGenreFilterDto>
    )
}