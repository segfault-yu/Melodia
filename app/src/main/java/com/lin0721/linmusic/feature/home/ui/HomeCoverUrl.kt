package com.lin0721.linmusic.feature.home.ui

// 区块页下发的封面地址并不统一：多数是裸地址，专属场景歌单那一类自带 ?imageView=1&thumbnail=8。
// 无脑追加 ?param= 会拼出两个问号的非法地址，故已带查询串的直接原样使用。
internal fun String.withCoverParam(param: String): String =
    if (contains('?')) this else "$this?param=$param"
