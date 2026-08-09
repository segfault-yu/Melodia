package com.lin0721.linmusic.core.api

import kotlinx.serialization.Serializable

@Serializable
data class HomepageBlockResponse(
    val code: Int = 0,
    val data: HomepageData? = null,
    val message: String? = null
)

@Serializable
data class HomepageData(
    val blocks: List<HomepageBlock> = emptyList(),
    val hasMore: Boolean = false,
    val cursor: String? = null
)

@Serializable
data class HomepageBlock(
    val blockCode: String = "",
    val showType: String = "",
    val uiElement: BlockUiElement? = null,
    val creatives: List<BlockCreative>? = null
)

@Serializable
data class BlockUiElement(
    val mainTitle: MainTitle? = null,
    val subTitle: MainTitle? = null
)

@Serializable
data class MainTitle(
    val title: String = ""
)

@Serializable
data class BlockCreative(
    val creativeId: String = "",
    val creativeType: String = "",
    val targetId: Long = 0,
    val uiElement: CreativeUiElement? = null,
    val resources: List<BlockResource>? = null
)

@Serializable
data class CreativeUiElement(
    val mainTitle: MainTitle? = null,
    val subTitle: MainTitle? = null,
    val image: CreativeImage? = null
)

@Serializable
data class CreativeImage(
    val imageUrl: String = ""
)

@Serializable
data class BlockResource(
    val resourceId: String = "",
    val resourceType: String = "",
    val targetId: Long = 0,
    val uiElement: CreativeUiElement? = null
)
