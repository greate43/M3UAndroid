package com.m3u.smartphone.ui.business.foryou.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.m3u.data.database.model.DataSource
import com.m3u.data.database.model.Playlist
import com.m3u.data.database.model.epgUrlsOrXtreamXmlUrl
import com.m3u.data.database.model.refreshable
import com.m3u.data.database.model.type
import com.m3u.i18n.R.string
import com.m3u.smartphone.ui.common.helper.Metadata
import com.m3u.smartphone.ui.common.internal.PreviewTheme
import com.m3u.smartphone.ui.material.ktx.plus
import com.m3u.smartphone.ui.material.model.LocalHazeState
import com.m3u.smartphone.ui.material.model.LocalSpacing
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlin.math.absoluteValue

@Composable
internal fun PlaylistGallery(
    rowCount: Int,
    headlineAspectRatio: Float,
    playlists: Map<Playlist, Int>,
    subscribingPlaylistUrls: List<String>,
    refreshingEpgUrls: List<String>,
    onClick: (Playlist) -> Unit,
    onLongClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    header: (@Composable () -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    val isInPreview = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val windowInfo = LocalWindowInfo.current
    val containerWidth = windowInfo.containerSize.width.coerceAtLeast(1)

    val state = rememberLazyGridState()
    val viewportStartOffset by remember {
        derivedStateOf {
            if (state.firstVisibleItemIndex == 0) state.firstVisibleItemScrollOffset
            else -Int.MAX_VALUE
        }
    }
    LaunchedEffect(containerWidth, isInPreview) {
        if (!isInPreview) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                snapshotFlow { viewportStartOffset }
                    .onEach {
                        Metadata.headlineFraction = it.absoluteValue
                            .times(headlineAspectRatio)
                            .div(containerWidth)
                            .coerceIn(0f, 1f)
                    }
                    .onCompletion { Metadata.headlineFraction = 1f }
                    .launchIn(this)
            }
        }
    }
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(rowCount),
        contentPadding = PaddingValues(vertical = spacing.medium) + contentPadding,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        modifier = if (isInPreview) modifier else modifier.hazeSource(LocalHazeState.current)
    ) {
        if (header != null) {
            item(span = { GridItemSpan(rowCount) }) {
                header()
            }
        }
        val entries = playlists.entries.toList()
        items(entries.size) { index ->
            val (playlist, count) = entries[index]
            val subscribing = playlist.url in subscribingPlaylistUrls
            val refreshing = playlist
                .epgUrlsOrXtreamXmlUrl()
                .any { it in refreshingEpgUrls }
            PlaylistItem(
                label = PlaylistGalleryDefaults.calculateUiTitle(
                    title = playlist.title,
                    refreshable = playlist.refreshable
                ),
                type = with(playlist) {
                    when (source) {
                        DataSource.M3U -> "$source"
                        DataSource.Xtream -> "$source $type"
                        else -> null
                    }
                },
                count = count,
                subscribingOrRefreshing = subscribing || refreshing,
                refreshable = playlist.refreshable,
                onClick = { onClick(playlist) },
                onLongClick = { onLongClick(playlist) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        PlaylistGalleryDefaults.calculateItemHorizontalPadding(
                            rowCount = rowCount,
                            index = index
                        )
                    )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlaylistGalleryPreview() {
    PreviewTheme {
        PlaylistGallery(
            rowCount = 2,
            headlineAspectRatio = Metadata.headlineAspectRatio(rail = false),
            playlists = linkedMapOf(
                Playlist(
                    title = "Live Sports Ultra HD",
                    url = "https://example.com/sports.m3u"
                ) to 182,
                Playlist(
                    title = "Cinema Night Archive",
                    url = Playlist.URL_IMPORTED
                ) to 64,
                Playlist(
                    title = "World News",
                    url = "https://demo.example.com/player_api.php?username=demo&password=demo&xtream_type=live",
                    source = DataSource.Xtream
                ) to 96,
                Playlist(
                    title = "Kids and Family",
                    url = "https://example.com/kids.m3u"
                ) to 41
            ),
            subscribingPlaylistUrls = listOf("https://example.com/sports.m3u"),
            refreshingEpgUrls = emptyList(),
            onClick = {},
            onLongClick = {},
            header = {
                Text(
                    text = "Your playlists",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            },
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize()
        )
    }
}

private object PlaylistGalleryDefaults {
    @Composable
    fun calculateUiTitle(title: String, refreshable: Boolean): String {
        val actual = title.ifEmpty {
            if (!refreshable) stringResource(string.feat_foryou_imported_playlist_title)
            else ""
        }
        return actual.uppercase()
    }

    @Composable
    fun calculateItemHorizontalPadding(
        rowCount: Int,
        index: Int,
        padding: Dp = LocalSpacing.current.medium
    ): PaddingValues {
        return PaddingValues(
            start = if (index % rowCount == 0) padding else 0.dp,
            end = if (index % rowCount == rowCount - 1) padding else 0.dp
        )
    }
}
