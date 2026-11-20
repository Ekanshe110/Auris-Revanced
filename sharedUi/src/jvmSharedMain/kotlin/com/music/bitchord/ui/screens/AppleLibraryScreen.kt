package com.music.bitchord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.AppleLibraryContent
import kotlinx.coroutines.launch
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.components.longPressMenuClickable
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.PullToRefresh
import com.music.bitchord.ui.components.feedSkeleton
import org.jetbrains.compose.resources.stringResource
import com.music.bitchord.sharedui.resources.*

/**
 * The Library page, in Apple's own shape.
 *
 * Apple's library is the listener's own — their playlists, their albums,
 * their songs — and under Apple's look the page shows Apple's library and
 * nothing else. A page that mixed another service's library into Apple's is
 * neither Apple's page nor the other one's.
 *
 * The order is Apple's: what was added recently first, as a row of tiles;
 * then the albums; then the songs; and the playlists last, as a list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleLibraryScreen(
    state: UiState<AppleLibraryContent>,
    listState: LazyListState,
    onItemClick: (ShelfItem) -> Unit,
    onSongClick: (Song) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    /**
     * One of the library's categories, opened as a page of its own the way
     * Apple's library does — "playlists", "albums", "artists" or "songs".
     */
    onCategoryClick: (String) -> Unit = {},
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    title: String? = null,
) {
    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            if (title != null) {
                item(key = "apple-library:title") {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(
                            start = PAGE_GUTTER,
                            end = PAGE_GUTTER,
                            top = 12.dp,
                            bottom = 14.dp,
                        ),
                    )
                }
            }
            when (val page = state) {
                is UiState.Loading -> feedSkeleton(firstIsHero = false)
                is UiState.Error -> item(key = "apple-library:error") {
                    MessageState(
                        message = page.message,
                        actionLabel = stringResource(Res.string.retry),
                        onAction = onRetry,
                    )
                }
                is UiState.Success -> {
                    val content = page.data
                    // Where each part of the page begins, so the rows at the
                    // head of it can take the listener there. Apple's own
                    // library opens with the sections themselves.
                    val titleRows = if (title != null) 1 else 0
                    val recentRows = content.recentlyAdded.chunked(2)
                    // Where the page stands after each part, so the rows at
                    // the head of it can take the listener to the right one.
                    var at = titleRows + 1
                    val recentIndex = if (content.recentlyAdded.isNotEmpty()) at + 1 else -1
                    if (content.recentlyAdded.isNotEmpty()) at += 1 + recentRows.size
                    val albumsIndex = if (content.albums.isNotEmpty()) at + 1 else -1
                    if (content.albums.isNotEmpty()) at += 2
                    val songsIndex = if (content.songs.isNotEmpty()) at + 1 else -1
                    if (content.songs.isNotEmpty()) at += 1 + content.songs.size
                    val playlistsIndex = if (content.playlists.isNotEmpty()) at + 1 else -1
                    // The categories, in Apple's own order and wording.
                    // The categories, in Apple's own order, each opening the
                    // page it names rather than scrolling this one. A grid
                    // that only scrolls is not a page.
                    item(key = "apple-library:categories") {
                        Column {
                            AppleLibraryCategoryRow(
                                label = stringResource(Res.string.apple_playlists),
                                onClick = { onCategoryClick("playlists") },
                            )
                            AppleLibraryCategoryRow(
                                label = stringResource(Res.string.apple_albums),
                                onClick = { onCategoryClick("albums") },
                            )
                            AppleLibraryCategoryRow(
                                label = stringResource(Res.string.artists),
                                onClick = { onCategoryClick("artists") },
                            )
                            AppleLibraryCategoryRow(
                                label = stringResource(Res.string.apple_songs),
                                onClick = { onCategoryClick("songs") },
                            )
                        }
                    }
                    if (content.recentlyAdded.isNotEmpty()) {
                        item(key = "apple-library:recent-header") {
                            AppleSectionHeader(stringResource(Res.string.apple_recently_added))
                        }
                        // Two across, as Apple's own grid is: a row of tiles
                        // is a shelf, and this one is a shelf of the
                        // listener's own newest playlists.
                        itemsIndexed(
                            content.recentlyAdded.chunked(2),
                            key = { index, row -> "apple-library:recent:$index:${row.first().browseId}" },
                        ) { _, row ->
                            AppleTileRow(
                                items = row,
                                onItemClick = onItemClick,
                                onItemLongPress = onItemLongPress,
                            )
                        }
                    }
                    if (content.albums.isNotEmpty()) {
                        item(key = "apple-library:albums-header") {
                            AppleSectionHeader(stringResource(Res.string.apple_albums))
                        }
                        item(key = "apple-library:albums") {
                            AppleTileRow(
                                items = content.albums,
                                onItemClick = onItemClick,
                                onItemLongPress = onItemLongPress,
                            )
                        }
                    }
                    if (content.songs.isNotEmpty()) {
                        item(key = "apple-library:songs-header") {
                            AppleSectionHeader(stringResource(Res.string.apple_songs))
                        }
                        // The key carries the row's place as well as its id:
                        // Apple's library can hold the same recording twice,
                        // and a lazy list that is given the same key twice
                        // crashes rather than drawing it.
                        itemsIndexed(
                            content.songs,
                            key = { index, song -> "apple-library:song:$index:${song.videoId}" },
                        ) { _, song ->
                            AppleLibrarySongRow(
                                song = song,
                                onClick = { onSongClick(song) },
                            )
                        }
                    }
                    if (content.playlists.isNotEmpty()) {
                        item(key = "apple-library:playlists-header") {
                            AppleSectionHeader(stringResource(Res.string.apple_playlists))
                        }
                        itemsIndexed(
                            content.playlists,
                            key = { index, playlist -> "apple-library:playlist:$index:${playlist.browseId}" },
                        ) { _, playlist ->
                            AppleLibraryRow(
                                item = playlist,
                                onClick = { onItemClick(playlist) },
                                onLongPress = onItemLongPress?.let { { it(playlist) } },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One of the library's categories, the way Apple's own library heads its
 * page: a row that says what is in it and takes the listener there.
 */
@Composable
private fun AppleLibraryCategoryRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PAGE_GUTTER, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "\u203A",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The heading over one section of the page. */
@Composable
private fun AppleSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(
            start = PAGE_GUTTER,
            end = PAGE_GUTTER,
            top = 18.dp,
            bottom = 8.dp,
        ),
    )
}

/**
 * A row of tiles, the way Apple draws the things it leads a page with.
 *
 * A row of large covers, each with its name under it: the playlist or album
 * itself, not a track from it.
 */
@Composable
private fun AppleTileRow(
    items: List<ShelfItem>,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items.forEach { item ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .longPressMenuClickable(
                        onClick = { onItemClick(item) },
                        onLongClick = onItemLongPress?.let { { it(item) } },
                    ),
            ) {
                AsyncImage(
                    model = item.thumbnailUrl?.artworkAt(CARD_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // A row of one tile is still two columns wide.
        repeat(2 - items.size) {
            Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * One of the listener's own songs.
 *
 * A song, not a collection: it plays where it stands rather than opening a
 * page of its own.
 */
@Composable
private fun AppleLibrarySongRow(
    song: Song,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PAGE_GUTTER, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.thumbnailUrl?.artworkAt(ROW_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (song.artist.isNotBlank()) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** One row of the library: the item's own cover and its name. */
@Composable
private fun AppleLibraryRow(    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = PAGE_GUTTER, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumbnailUrl?.artworkAt(ROW_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
