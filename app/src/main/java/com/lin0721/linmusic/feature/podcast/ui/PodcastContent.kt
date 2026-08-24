package com.lin0721.linmusic.feature.podcast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.auth.UserProfile
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.GradientStart
import com.lin0721.linmusic.feature.home.ui.ErrorContent
import com.lin0721.linmusic.feature.home.ui.FilterPills
import com.lin0721.linmusic.feature.home.ui.LoadingIndicator
import com.lin0721.linmusic.feature.home.ui.TopGreetingBar
import com.lin0721.linmusic.feature.podcast.domain.PodcastRadio

// 「播客」tab 骨架：节目在上（点了即听），电台货架在下（点进详情页看整个系列）
@Composable
fun PodcastContent(
    uiState: PodcastUiState,
    userProfile: UserProfile?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAvatarClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCategorySelect: (Long?) -> Unit,
    onProgramClick: (Int) -> Unit,
    onRadioClick: (PodcastRadio) -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 180.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(GradientStart, BackgroundDark)))
                    .statusBarsPadding()
            ) {
                TopGreetingBar(
                    userProfile = userProfile,
                    onLoginClick = onAvatarClick,
                    onSearchClick = onSearchClick
                )
                FilterPills(selectedIndex = selectedTab, onSelected = onTabSelected)
            }
        }

        when (uiState) {
            is PodcastUiState.Loading -> item { LoadingIndicator() }

            is PodcastUiState.Error -> item {
                ErrorContent(message = uiState.message, onRetry = onRetry)
            }

            is PodcastUiState.Success -> {
                val data = uiState.data

                if (data.categories.isNotEmpty()) {
                    item {
                        PodcastCategoryChips(
                            categories = data.categories,
                            selectedId = data.selectedCategoryId,
                            onSelect = onCategorySelect
                        )
                    }
                }

                item { PodcastSectionTitle("最新节目", "点击即听") }

                if (data.isProgramLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = data.programs,
                        key = { index, program -> "${program.id}_$index" }
                    ) { index, program ->
                        PodcastProgramRow(program = program, onClick = { onProgramClick(index) })
                    }
                }

                // 未登录时该段为空，整块不出现
                if (data.personalizedRadios.isNotEmpty()) {
                    item { PodcastSectionTitle("猜你喜欢", "根据你的收听") }
                    item { PodcastRadioRow(radios = data.personalizedRadios, onClick = onRadioClick) }
                }

                if (data.recommendRadios.isNotEmpty()) {
                    item { PodcastSectionTitle("精选电台") }
                    item { PodcastRadioRow(radios = data.recommendRadios, onClick = onRadioClick) }
                }

                if (data.toplistRadios.isNotEmpty()) {
                    item { PodcastSectionTitle("热门电台榜") }
                    item {
                        PodcastRadioRow(
                            radios = data.toplistRadios,
                            showRank = true,
                            onClick = onRadioClick
                        )
                    }
                }
            }
        }
    }
}
