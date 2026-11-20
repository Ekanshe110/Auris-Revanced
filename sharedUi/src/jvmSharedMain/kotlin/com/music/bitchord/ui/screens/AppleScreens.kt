package com.music.bitchord.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.HEADER_ART_PX
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.components.longPressMenuClickable
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.AppleTokens
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.PullToRefresh
import com.music.bitchord.ui.components.feedSkeleton
import org.jetbrains.compose.resources.stringResource
import com.music.bitchord.sharedui.resources.*
import androidx.compose.foundation.shape.CircleShape

/**
 * The New page, in Apple's own shape.
 *
 * Apple Music's New tab is not a list of shelves with a heading over each: it
 * is one large card at the head of the page, a grid of two-column cards under
 * it, and then a plain list of what is left. The order is the shape, so it
 * is drawn in that order rather than being approximated with the ordinary
 * shelf rows.
 *
 * The rows themselves are Apple's rows — they come from the Apple catalogue
 * through the installed manifest, whatever else the app is playing — so this
 * page is the *look* axis: Apple's layout over whatever the recommendation
 * axis supplied.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleHomeScreen(
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    currentSong: Song?,
    isPlaying: Boolean,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
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
                item(key = "apple:title") {
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
                is UiState.Error -> item(key = "apple:error") {
                    MessageState(
                        message = page.message,
                        actionLabel = stringResource(Res.string.retry),
                        onAction = onRetry,
                    )
                }
                is UiState.Success -> {
                    val shelves = page.data
                    // The head of the page: a run of large portrait cards,
                    // the way Apple's own page opens. One card per group, and
                    // the first card is the one the page leads with.
                    val heads = shelves.take(6)
                    if (heads.isNotEmpty()) {
                        item(key = "apple:hero") {
                            AppleHeroCarousel(
                                shelves = heads,
                                onItemClick = onItemClick,
                                onItemLongPress = onItemLongPress,
                            )
                        }
                    }
                    // Every other group of Apple's, each as its own row: a
                    // heading over a horizontal run of square cards. That is
                    // the shape Apple's own page draws — one row per group,
                    // scrolled sideways, not a grid of everything at once.
                    itemsIndexed(
                        // Everything after the head cards: the groups the
                        // head did not already show.
                        shelves.filter { shelf -> heads.none { it === shelf } },
                        // The row's place is part of the key: two groups can
                        // carry the same title, and a lazy list given the
                        // same key twice crashes rather than drawing it.
                        key = { index, shelf -> "apple:shelf:$index:${shelf.title}" },
                    ) { _, shelf ->
                        AppleShelfCarousel(
                            shelf = shelf,
                            onItemClick = onItemClick,
                            onItemLongPress = onItemLongPress,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The New page, in Apple's own shape.
 *
 * Apple's New tab is the browse page: every group Apple offers, drawn as a
 * two-column grid of cards the way Apple's own browse page draws them. The
 * Home page leads with one large card and a few of these; this page is the
 * whole of Apple's browse, which is what the tab is for.
 *
 * The content is Apple's own groups, read from Apple's catalogue through the
 * installed manifest — so this page is Apple's whatever else the app is
 * playing, and it is a page of the catalogue rather than a page of the
 * listener's history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleBrowseScreen(
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    /** The directory of what else there is to browse, and the way into it. */
    directory: List<Pair<String, String>> = emptyList(),
    onBrowse: (String) -> Unit = { },
    /**
     * Tapping a group's heading. The chevron after the title says the row is
     * a way into the whole group, so the heading has to answer a tap.
     */
    onShelfHeaderClick: ((HomeShelf) -> Unit)? = null,
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
                item(key = "apple-browse:title") {
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
                is UiState.Error -> item(key = "apple-browse:error") {
                    MessageState(
                        message = page.message,
                        actionLabel = stringResource(Res.string.retry),
                        onAction = onRetry,
                    )
                }
                is UiState.Success -> {
                    // Apple's browse page is a run of groups, each a heading
                    // over a horizontal row of square cards.
                    itemsIndexed(
                        page.data,
                        key = { index, shelf -> "apple-browse:shelf:$index:${shelf.title}" },
                    ) { _, shelf ->
                        AppleShelfCarousel(
                            shelf = shelf,
                            onItemClick = onItemClick,
                            onItemLongPress = onItemLongPress,
                            onShelfHeaderClick = onShelfHeaderClick,
                        )
                    }
                    // The directory of what else there is to browse, the way
                    // Apple's own browse page ends.
                    if (directory.isNotEmpty()) {
                        item(key = "apple-browse:directory-header") {
                            Text(
                                text = stringResource(Res.string.more_to_explore),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(
                                    start = PAGE_GUTTER,
                                    end = PAGE_GUTTER,
                                    top = 20.dp,
                                    bottom = 8.dp,
                                ),
                            )
                        }
                        itemsIndexed(
                            directory,
                            key = { index, row -> "apple-browse:directory:$index:${row.first}" },
                        ) { _, row ->
                            AppleDirectoryRow(label = row.first, onClick = { onBrowse(row.first) })
                        }
                    }
                }
            }
        }
    }
}

/**
 * One group of Apple's, as its own row.
 *
 * A heading over a horizontal run of square cards — the shape every group
 * on Apple's own page has. The heading carries the chevron Apple's headings
 * carry, because the row is a way into the group and not a dead end.
 */
@Composable
private fun AppleShelfCarousel(
    shelf: HomeShelf,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    onShelfHeaderClick: ((HomeShelf) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            // Apple's headings carry a chevron, because a heading over a row
            // is a way into more of it rather than a label — and it is the
            // heading that is tapped, not just the arrow after it.
            text = shelf.title + "  \u203A",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onShelfHeaderClick != null) {
                    onShelfHeaderClick?.invoke(shelf)
                }
                .padding(
                    start = PAGE_GUTTER,
                    end = PAGE_GUTTER,
                    top = 18.dp,
                    bottom = 8.dp,
                ),
        )
        // A shelf of songs is a list, not a row of cards: Apple draws "Best
        // New Songs" as tracks with a cover, a title and an artist, because
        // songs are played one at a time and scanned vertically. A cover you
        // swipe past says nothing about which song it is.
        if (shelf.items.isNotEmpty() && shelf.items.all { it.videoId != null }) {
            shelf.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item, shelf.title) }
                        .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(AppleTokens.CornerSmall))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        if (item.thumbnailUrl != null) {
                            AsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
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
            return@Column
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(
                shelf.items,
                key = { index, item -> "apple:card:$index:${item.videoId ?: item.title}" },
            ) { _, item ->
                Column(
                    modifier = Modifier.width(150.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .longPressMenuClickable(
                                onClick = { onItemClick(item, shelf.title) },
                                onLongClick = onItemLongPress?.let { { it(item) } },
                                enabled = item.videoId != null || item.browseId != null || item.externalUrl != null,
                            ),
                    ) {
                        AsyncImage(
                            model = item.thumbnailUrl?.artworkAt(CARD_ART_PX),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
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
        }
    }
}

/**
 * One row of the directory.
 *
 * A plain line of words, the way Apple's own browse page ends: the way into
 * the rest of the catalogue.
 */
@Composable
private fun AppleDirectoryRow(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PAGE_GUTTER, vertical = 13.dp),
    )
}

/**
 * The run of large cards at the head of the page.
 *
 * Apple's own page opens with portrait cards: the cover, the title over it
 * and the artist under it. They are a run rather than one card, because the
 * page leads with several of the listener's own things.
 */
@Composable
private fun AppleHeroCarousel(
    shelves: List<HomeShelf>,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(
            shelves,
            key = { index, shelf -> "apple:hero:$index:${shelf.title}" },
        ) { _, shelf ->
            val item = shelf.items.firstOrNull() ?: return@itemsIndexed
            Column(
                modifier = Modifier.width(240.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .longPressMenuClickable(
                            onClick = { onItemClick(item, shelf.title) },
                            onLongClick = onItemLongPress?.let { { it(item) } },
                        ),
                ) {
                    AsyncImage(
                        model = item.thumbnailUrl?.artworkAt(HEADER_ART_PX),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                    )
                }
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
    }
}

/**
 * The large card at the head of the page.
 *
 * A cover with the title laid over it, the way Apple's own head card is
 * drawn — the artwork is the card, not a picture inside a card.
 */
/**
 * The two-column grid of cards.
 *
 * A row of two columns rather than a nested lazy grid: the page is a lazy
 * column already, and a grid inside one is a second scrolling surface for no
 * gain — there are four cards here, and they all fit.
 */
@Composable
private fun AppleShelfGrid(
    shelves: List<HomeShelf>,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        shelves.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                pair.forEach { shelf ->
                    val item = shelf.items.firstOrNull()
                    AppleGridCard(
                        shelf = shelf,
                        item = item,
                        onClick = { item -> onItemClick(item, shelf.title) },
                        onLongPress = onItemLongPress,
                        modifier = Modifier.weight(1f),
                    )
                }
                // A row of one card, when the number of them is odd, still
                // has to be two columns wide.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** One card of the grid: a cover with the shelf's own name under it. */
@Composable
private fun AppleGridCard(
    shelf: HomeShelf,
    item: ShelfItem?,
    onClick: (ShelfItem) -> Unit,
    onLongPress: ((ShelfItem) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (item != null) {
                        Modifier.longPressMenuClickable(
                            onClick = { onClick(item) },
                            onLongClick = onLongPress?.let { { it(item) } },
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (item != null) {
                AsyncImage(
                    model = item.thumbnailUrl?.artworkAt(CARD_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = shelf.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!shelf.subtitle.isBlank()) {
            Text(
                text = shelf.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One row of the "More to Explore" list. */
@Composable
private fun AppleExploreRow(
    item: ShelfItem,
    shelfTitle: String,
    onClick: (ShelfItem) -> Unit,
    onLongPress: ((ShelfItem) -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .longPressMenuClickable(
                onClick = { onClick(item) },
                onLongClick = onLongPress?.let { { it(item) } },
            )
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumbnailUrl?.artworkAt(ROW_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle.ifBlank { shelfTitle },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
