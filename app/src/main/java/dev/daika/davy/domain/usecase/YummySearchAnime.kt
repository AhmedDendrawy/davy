package dev.daika.davy.domain.usecase

import dev.daika.davy.data.repository.YummyRepository
import dev.daika.davy.domain.entity.AnimeFilterState
import javax.inject.Inject

class YummySearchAnime @Inject constructor(private val repository: YummyRepository) {
    operator fun invoke(query: String, filter: AnimeFilterState) =
        repository.searchAnime(query, filter)
}