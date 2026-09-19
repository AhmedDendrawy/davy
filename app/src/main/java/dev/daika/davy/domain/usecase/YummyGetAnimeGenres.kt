package dev.daika.davy.domain.usecase

import dev.daika.davy.data.repository.YummyRepository
import dev.daika.davy.domain.entity.AnimeGenreFilter
import javax.inject.Inject

class YummyGetAnimeGenres @Inject constructor(private val repository: YummyRepository) {
    suspend operator fun invoke(): List<AnimeGenreFilter> =
        repository.getAnimeGenres()
}
