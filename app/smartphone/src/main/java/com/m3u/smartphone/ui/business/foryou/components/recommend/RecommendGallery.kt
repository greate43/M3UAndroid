package com.m3u.smartphone.ui.business.foryou.components.recommend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.m3u.business.foryou.Recommend
import com.m3u.core.wrapper.eventOf
import com.m3u.data.database.model.Channel
import com.m3u.data.database.model.DataSource
import com.m3u.data.database.model.Playlist
import com.m3u.smartphone.ui.common.internal.PreviewTheme
import com.m3u.smartphone.ui.common.internal.Events
import com.m3u.smartphone.ui.material.components.HorizontalPagerIndicator
import com.m3u.smartphone.ui.material.ktx.pageOffset
import com.m3u.smartphone.ui.material.model.LocalSpacing
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Composable
internal fun RecommendGallery(
    specs: List<Recommend.Spec>,
    onPlayChannel: (Channel) -> Unit,
    navigateToPlaylist: (Playlist) -> Unit,
    onSpecChanged: (Recommend.Spec?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (specs.isEmpty()) {
        return
    }

    val spacing = LocalSpacing.current
    val uriHandler = LocalUriHandler.current

    val onClick = { spec: Recommend.Spec ->
        when (spec) {
            is Recommend.UnseenSpec -> {
                onPlayChannel(spec.channel)
            }

            is Recommend.DiscoverSpec -> {
                Events.discoverCategory = eventOf(spec.category)
                navigateToPlaylist(spec.playlist)
            }

            is Recommend.CwSpec -> {
                onPlayChannel(spec.channel)
            }

            is Recommend.NewRelease -> {
                uriHandler.openUri(spec.url)
            }
        }
    }

    val state = rememberPagerState { specs.size }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        DisposableEffect(state.currentPage) {
            onSpecChanged(specs[state.currentPage])
            onDispose {
                onSpecChanged(null)
            }
        }
        HorizontalPager(
            state = state,
            contentPadding = PaddingValues(horizontal = spacing.medium),
            modifier = Modifier.height(128.dp)
        ) { page ->
            val spec = specs[page]
            val pageOffset = state.pageOffset(page)
            RecommendItem(
                spec = spec,
                pageOffset = pageOffset,
                onClick = { onClick(spec) }
            )
        }
        HorizontalPagerIndicator(
            pagerState = state,
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = spacing.medium),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 240)
@Composable
private fun RecommendGalleryPreview() {
    val now = Clock.System.now().toEpochMilliseconds()
    val playlist = Playlist(
        title = "Live Sports Ultra HD",
        url = "https://example.com/sports.m3u",
        source = DataSource.M3U
    )
    PreviewTheme {
        RecommendGallery(
            specs = listOf(
                Recommend.CwSpec(
                    channel = Channel(
                        id = 1,
                        url = "https://example.com/channel/1.m3u8",
                        category = "Sports",
                        title = "Liverpool TV",
                        cover = "https://images.unsplash.com/photo-1547347298-4074fc3086f0",
                        playlistUrl = playlist.url
                    ),
                    position = 27 * 60 * 1000L
                ),
                Recommend.UnseenSpec(
                    channel = Channel(
                        id = 2,
                        url = "https://example.com/channel/2.m3u8",
                        category = "Drama",
                        title = "Midnight Cinema",
                        cover = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba",
                        playlistUrl = playlist.url,
                        seen = now - 3.days.inWholeMilliseconds
                    )
                )
            ),
            onPlayChannel = {},
            navigateToPlaylist = {},
            onSpecChanged = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}
