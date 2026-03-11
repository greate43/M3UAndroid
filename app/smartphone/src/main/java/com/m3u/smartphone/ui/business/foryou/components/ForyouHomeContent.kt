package com.m3u.smartphone.ui.business.foryou.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m3u.business.foryou.Recommend
import com.m3u.core.util.basic.title
import com.m3u.data.database.model.Channel
import com.m3u.data.database.model.DataSource
import com.m3u.data.database.model.Playlist
import com.m3u.data.database.model.type
import com.m3u.data.parser.xtream.XtreamInput
import com.m3u.i18n.R.string
import com.m3u.smartphone.ui.business.foryou.components.recommend.RecommendGallery
import com.m3u.smartphone.ui.business.playlist.components.ChannelItem
import com.m3u.smartphone.ui.common.helper.Metadata
import com.m3u.smartphone.ui.common.internal.PreviewTheme
import com.m3u.smartphone.ui.material.components.TextBadge
import com.m3u.smartphone.ui.material.ktx.plus
import com.m3u.smartphone.ui.material.model.LocalSpacing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Composable
internal fun ForyouHomeContent(
    rowCount: Int,
    playlists: Map<Playlist, Int>,
    specs: List<Recommend.Spec>,
    contentPadding: PaddingValues,
    navigateToPlaylist: (Playlist) -> Unit,
    onPlayChannel: (Channel) -> Unit,
    observeChannels: (String) -> Flow<List<Channel>>,
    onSpecChanged: (Recommend.Spec?) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val playlistGroups = remember(playlists) { playlists.toPlaylistGroups() }

    var selectedGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(ForyouContentTab.Live) }

    LaunchedEffect(playlistGroups) {
        val selected = playlistGroups.firstOrNull { it.id == selectedGroupId } ?: playlistGroups.firstOrNull()
        selectedGroupId = selected?.id
        selectedTab = selected?.tabs?.firstOrNull() ?: ForyouContentTab.Live
    }

    val selectedGroup = remember(playlistGroups, selectedGroupId) {
        playlistGroups.firstOrNull { it.id == selectedGroupId } ?: playlistGroups.firstOrNull()
    }

    LaunchedEffect(selectedGroup?.id) {
        selectedGroup?.let { group ->
            if (selectedTab !in group.tabs) {
                selectedTab = group.tabs.firstOrNull() ?: ForyouContentTab.Live
            }
        }
    }

    val selectedPlaylist = remember(selectedGroup, selectedTab) {
        selectedGroup?.playlistsByTab?.get(selectedTab)?.playlist
    }
    val channels by selectedPlaylist
        ?.let { observeChannels(it.url) }
        ?.collectAsStateWithLifecycle(initialValue = emptyList())
        ?: remember { mutableStateOf(emptyList()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.large),
        modifier = modifier
            .fillMaxSize()
            .padding(top = spacing.small)
    ) {
        if (specs.isNotEmpty()) {
            RecommendGallery(
                specs = specs,
                navigateToPlaylist = navigateToPlaylist,
                onPlayChannel = onPlayChannel,
                onSpecChanged = onSpecChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (playlistGroups.isNotEmpty()) {
            PlaylistSwitcher(
                playlistGroups = playlistGroups,
                selectedGroupId = selectedGroup?.id,
                onSelect = { group ->
                    selectedGroupId = group.id
                    selectedTab = group.tabs.firstOrNull() ?: ForyouContentTab.Live
                },
                modifier = Modifier.fillMaxWidth()
            )
            selectedGroup?.let { group ->
                ContentTabs(
                    tabs = group.tabs,
                    selectedTab = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        ContentHeader(
            selectedGroup = selectedGroup,
            selectedTab = selectedTab,
            channelCount = channels.size,
            modifier = Modifier.padding(horizontal = spacing.medium)
        )

        ChannelGrid(
            rowCount = rowCount,
            channels = channels,
            isPosterGrid = selectedTab != ForyouContentTab.Live,
            onPlayChannel = onPlayChannel,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PlaylistSwitcher(
    playlistGroups: List<ForyouPlaylistGroup>,
    selectedGroupId: String?,
    onSelect: (ForyouPlaylistGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        contentPadding = PaddingValues(horizontal = spacing.medium),
        modifier = modifier
    ) {
        items(playlistGroups, key = ForyouPlaylistGroup::id) { group ->
            FilterChip(
                selected = group.id == selectedGroupId,
                onClick = { onSelect(group) },
                label = {
                    Text(
                        text = group.label.title(),
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    TextBadge(
                        text = group.availableTabs.size.toString(),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            )
        }
    }
}

@Composable
private fun ContentTabs(
    tabs: List<ForyouContentTab>,
    selectedTab: ForyouContentTab,
    onSelect: (ForyouContentTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onSelect(tab) },
                text = { Text(stringResource(tab.label).title()) }
            )
        }
    }
}

@Composable
private fun ContentHeader(
    selectedGroup: ForyouPlaylistGroup?,
    selectedTab: ForyouContentTab,
    channelCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Text(
            text = selectedGroup?.label?.title()
                ?: stringResource(string.feat_foryou_playlists_section_title).title(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (selectedGroup == null) {
                stringResource(string.feat_foryou_provider_empty).title()
            } else {
                "${stringResource(selectedTab.label).title()} • $channelCount titles"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChannelGrid(
    rowCount: Int,
    channels: List<Channel>,
    isPosterGrid: Boolean,
    onPlayChannel: (Channel) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val actualColumns = if (isPosterGrid) rowCount + 1 else rowCount

    if (channels.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
        ) {
            Text(
                text = stringResource(string.feat_foryou_provider_empty).title(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(actualColumns.coerceAtLeast(1)),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(horizontal = spacing.medium, vertical = spacing.medium) + contentPadding,
        modifier = modifier
    ) {
        items(channels, key = Channel::id) { channel ->
            ChannelItem(
                channel = channel,
                recently = false,
                zapping = false,
                cover = channel.cover,
                programme = null,
                isVodOrSeriesPlaylist = isPosterGrid,
                onClick = { onPlayChannel(channel) },
                onLongClick = { onPlayChannel(channel) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private enum class ForyouContentTab(val label: Int) {
    Live(string.feat_foryou_tab_live),
    Movies(string.feat_foryou_tab_movies),
    Shows(string.feat_foryou_tab_shows)
}

private data class ForyouPlaylistEntry(
    val playlist: Playlist,
    val count: Int
)

private data class ForyouPlaylistGroup(
    val id: String,
    val label: String,
    val playlistsByTab: Map<ForyouContentTab, ForyouPlaylistEntry>
) {
    val availableTabs: List<ForyouContentTab> = playlistsByTab.keys.toList()
    val tabs: List<ForyouContentTab> = playlistsByTab.keys.toList()
}

private fun Map<Playlist, Int>.toPlaylistGroups(): List<ForyouPlaylistGroup> {
    return entries
        .groupBy(
            keySelector = { (playlist, _) -> playlist.groupId() },
            valueTransform = { ForyouPlaylistEntry(it.key, it.value) }
        )
        .mapNotNull { (id, entries) ->
            val playlistsByTab = entries
                .associateBy { it.playlist.toContentTab() }
                .toSortedMap(compareBy(ForyouContentTab::ordinal))
            if (playlistsByTab.isEmpty()) null
            else ForyouPlaylistGroup(
                id = id,
                label = entries.firstOrNull()?.playlist?.title.orEmpty(),
                playlistsByTab = playlistsByTab
            )
        }
        .sortedBy(ForyouPlaylistGroup::label)
}

private fun Playlist.groupId(): String {
    if (source == DataSource.Xtream) {
        val input = XtreamInput.decodeFromPlaylistUrlOrNull(url)
        if (input != null) {
            return listOf("xtream", input.basicUrl, input.username).joinToString("|")
        }
    }
    return "playlist|$url"
}

private fun Playlist.toContentTab(): ForyouContentTab {
    return when {
        source == DataSource.Xtream && type == DataSource.Xtream.TYPE_SERIES -> ForyouContentTab.Shows
        source == DataSource.Xtream && type == DataSource.Xtream.TYPE_VOD -> ForyouContentTab.Movies
        else -> ForyouContentTab.Live
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ForyouHomeContentPreview() {
    val now = Clock.System.now().toEpochMilliseconds()
    val atlasLive = Playlist(
        title = "Atlas Stream",
        url = "https://provider.example.com/player_api.php?username=atlas&password=123&xtream_type=live",
        source = DataSource.Xtream
    )
    val atlasMovies = Playlist(
        title = "Atlas Stream",
        url = "https://provider.example.com/player_api.php?username=atlas&password=123&xtream_type=vod",
        source = DataSource.Xtream
    )
    val atlasShows = Playlist(
        title = "Atlas Stream",
        url = "https://provider.example.com/player_api.php?username=atlas&password=123&xtream_type=series",
        source = DataSource.Xtream
    )
    PreviewTheme {
        ForyouHomeContent(
            rowCount = 2,
            playlists = linkedMapOf(
                atlasLive to 182,
                atlasMovies to 64,
                atlasShows to 96
            ),
            specs = listOf(
                Recommend.CwSpec(
                    channel = Channel(
                        id = 1,
                        url = "https://example.com/channel/1.m3u8",
                        category = "Sports",
                        title = "Liverpool TV",
                        cover = null,
                        playlistUrl = atlasLive.url
                    ),
                    position = 27 * 60 * 1000L
                ),
                Recommend.UnseenSpec(
                    channel = Channel(
                        id = 2,
                        url = "https://example.com/channel/2.m3u8",
                        category = "Drama",
                        title = "Prison Break",
                        cover = null,
                        playlistUrl = atlasShows.url,
                        seen = now - 4.days.inWholeMilliseconds
                    )
                )
            ),
            contentPadding = PaddingValues(16.dp),
            navigateToPlaylist = {},
            onPlayChannel = {},
            observeChannels = {
                flowOf(
                    listOf(
                        Channel(
                            id = 20,
                            url = "https://example.com/show/20",
                            category = "Shows",
                            title = "Prison Break",
                            cover = null,
                            playlistUrl = atlasShows.url
                        ),
                        Channel(
                            id = 21,
                            url = "https://example.com/show/21",
                            category = "Shows",
                            title = "Breaking Bad",
                            cover = null,
                            playlistUrl = atlasShows.url
                        )
                    )
                )
            },
            onSpecChanged = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
