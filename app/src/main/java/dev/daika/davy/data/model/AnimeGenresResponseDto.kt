package dev.daika.davy.data.model

import dev.daika.davy.domain.entity.AnimeGenreFilter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeGenresResponseDto(
    val genres: List<AnimeGenreFilterDto>,
    val groups: List<AnimeGenreGroupDto>
)

@Serializable
data class AnimeGenreFilterDto(
    val title: String,
    val href: String,
    val value: Int,
    @SerialName("more_titles")
    val moreTitles: List<String>,
    @SerialName("group_id")
    val groupId: Int
) {
    fun toEntity() = AnimeGenreFilter(
        id = href,
        title = title
    )
}

@Serializable
data class AnimeGenreGroupDto(
    val title: String,
    val id: Int
)
