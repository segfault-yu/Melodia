package com.lin0721.linmusic.feature.search.data.dto

import kotlinx.serialization.Serializable

// /weapi/search/suggest/keyword 真机核实：无结果时 result 为空对象，allMatch 字段缺省
@Serializable
data class SearchSuggestResponse(
    val code: Int = 0,
    val result: SearchSuggestResult? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class SearchSuggestResult(
    val allMatch: List<SearchSuggestKeyword>? = null
)

@Serializable
data class SearchSuggestKeyword(
    val keyword: String = ""
)

@Serializable
data class SearchSuggestRequest(
    val s: String
)
