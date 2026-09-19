package dev.daika.davy.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.daika.davy.domain.entity.ALL_GENRES
import dev.daika.davy.domain.entity.Anime
import dev.daika.davy.domain.entity.AnimeFilterState
import dev.daika.davy.domain.entity.FilterOption
import dev.daika.davy.domain.usecase.YummyGetAnimeGenres
import dev.daika.davy.domain.usecase.YummySearchAnime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    private val yummySearchAnime: YummySearchAnime,
    private val yummyGetAnimeGenres: YummyGetAnimeGenres
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _genreOptions = MutableStateFlow(ALL_GENRES)
    val genreOptions = _genreOptions.asStateFlow()
    private val _filterState = MutableStateFlow(AnimeFilterState())
    val filterState = _filterState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedItems: Flow<PagingData<Anime>> =
        combine(
            searchQuery,
            filterState
        ) { query, filter -> query to filter }.flatMapLatest { (query, filter) ->
            yummySearchAnime(query, filter)
        }.cachedIn(viewModelScope)

    init {
        loadGenres()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterState(filterState: AnimeFilterState) {
        _filterState.value = filterState
    }

    private fun loadGenres() {
        viewModelScope.launch {
            _genreOptions.value = yummyGetAnimeGenres().map { genre ->
                FilterOption(
                    id = genre.id,
                    title = genre.title
                )
            }
        }
    }
}
