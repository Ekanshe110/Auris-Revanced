package com.music.bitchord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.music.bitchord.data.model.AppleRadioRow
import com.music.bitchord.data.model.AppleRadioShelf
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.RadioStation
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.AppleTokens
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.PullToRefresh
import com.music.bitchord.ui.components.feedSkeleton
import org.jetbrains.compose.resources.stringResource
import com.music.bitchord.sharedui.resources.*

/**
 * The Radio page, in Apple's own shape.
 *
 * Apple's radio is its stations and nothing else: the live ones, marked LIVE
 * and played here, and the seeded ones, which are queues of songs and play
 * here too.
 *
 * The page is laid out the way Apple's own is: the grid of station logos at
 * the head of it, then a row of stations by what they play, then the
 * international broadcasters, and then the stations themselves.
 *
 * There is no Flow tile and no "search a station" row. Neither is Apple's,
 * and a page that offers them is not Apple's page — they belong to the other
 * look, where they are the page's own subjects.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleRadioScreen(
    state: UiState<List<RadioStation>>,
    international: UiState<List<RadioStation>>,
    shelves: List<AppleRadioShelf> = emptyList(),
    listState: LazyListState,
    onStationClick: (RadioStation) -> Unit,
    onRowClick: (AppleRadioRow) -> Unit = { },
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
                item(key = "apple-radio:title") {
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
                is UiState.Error -> item(key = "apple-radio:error") {
                    MessageState(
                        message = page.message,
                        actionLabel = stringResource(Res.string.retry),
                        onAction = onRetry,
                    )
                }
                is UiState.Success -> {
                    val stations = page.data
                    // The grid of station logos at the head of the page, the
                    // way Apple's own Radio page opens: three across, two
                    // down, and nothing else above them.
                    val live = stations.filter { it.isLive }
                    if (live.isNotEmpty()) {
                        items(live.chunked(3), key = { "apple-radio:grid:${it.first().id}" }) { row ->
                            AppleStationGridRow(
                                stations = row,
                                onStationClick = onStationClick,
                            )
                        }
                        // "On Air Now": the broadcast itself, in a large
                        // card with the station's name under it. Apple's own
                        // page carries the live schedule here; the schedule
                        // is Apple's and not published, so the card names the
                        // station and says who broadcasts it.
                        item(key = "apple-radio:onair-header") {
                            AppleRadioHeader(stringResource(Res.string.apple_on_air_now))
                        }
                        item(key = "apple-radio:onair") {
                            AppleOnAirRow(
                                stations = live,
                                onStationClick = onStationClick,
                            )
                        }
                        // The stations by what they play: Apple's own row of
                        // genre stations, which are live in the same way.
                        item(key = "apple-radio:genre-header") {
                            AppleRadioHeader(stringResource(Res.string.apple_stations_by_genre))
                        }
                        item(key = "apple-radio:genre") {
                            AppleStationCarousel(
                                stations = live,
                                onStationClick = onStationClick,
                            )
                        }
                    }
                    // Apple's own sections: the stations by genre, the
                    // sessions, the mixes, the interviews and the shows.
                    shelves.forEach { shelf ->
                        item(key = "apple-radio:shelf:${shelf.title}") {
                            AppleRadioHeader(shelf.title)
                        }
                        item(key = "apple-radio:shelf:${shelf.title}:row") {
                            AppleRadioCarousel(
                                rows = shelf.items,
                                onRowClick = onRowClick,
                            )
                        }
                    }
                    // The international broadcasters: other people's stations,
                    // live in the same way, with their own names under them.
                    val others = (international as? UiState.Success)?.data.orEmpty()
                    if (others.isNotEmpty()) {
                        item(key = "apple-radio:international-header") {
                            AppleRadioHeader(stringResource(Res.string.apple_international_broadcasters))
                        }
                        item(key = "apple-radio:international") {
                            AppleStationCarousel(
                                stations = others,
                                onStationClick = onStationClick,
                            )
                        }
                    }
                    items(stations, key = { "apple-radio:${it.id}" }) { station ->
                        AppleStationRow(station = station, onClick = { onStationClick(station) })
                    }
                }
            }
        }
    }
}

/** The heading over one of the page's rows, with the chevron Apple's have. */
@Composable
private fun AppleRadioHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(
            start = PAGE_GUTTER,
            end = PAGE_GUTTER,
            top = 20.dp,
            bottom = 10.dp,
        ),
    )
}

/**
 * One row of the grid of station logos.
 *
 * Three tiles across, each a square of the station's own artwork — the shape
 * Apple's own Radio page opens with, where the logo is the whole of the
 * tile and the name is on the station's own page.
 */
@Composable
private fun AppleStationGridRow(
    stations: List<RadioStation>,
    onStationClick: (RadioStation) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        stations.forEach { station ->
            AsyncImage(
                model = station.artworkUrl?.artworkAt(CARD_ART_PX),
                contentDescription = station.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onStationClick(station) },
            )
        }
        // A row of two tiles is still three columns wide.
        repeat(3 - stations.size) {
            Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * The broadcast itself: a large card with the station's logo and its name.
 *
 * The card is the size Apple's own is — nearly the width of the page — so
 * that the station being heard is the thing the eye lands on.
 */
@Composable
private fun AppleOnAirRow(
    stations: List<RadioStation>,
    onStationClick: (RadioStation) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(stations, key = { "apple-radio:onair:${it.id}" }) { station ->
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .clickable { onStationClick(station) },
            ) {
                AsyncImage(
                    model = station.artworkUrl?.artworkAt(CARD_ART_PX),
                    contentDescription = station.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        // `live_radio_tile.xml`: a square tile at
                        // @dimen/radio_live_tile_corner_radius (20dp) with a
                        // 0.5dp @color/rounded_image_stroke_color hairline.
                        .clip(RoundedCornerShape(AppleTokens.LiveTileRadius))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 0.5.dp,
                            color = Color(0x1A000000),
                            shape = RoundedCornerShape(AppleTokens.LiveTileRadius),
                        ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = station.provider ?: stringResource(Res.string.live),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * A horizontal run of stations: the artwork, the station's name, and who
 * broadcasts it.
 */
@Composable
private fun AppleStationCarousel(
    stations: List<RadioStation>,
    onStationClick: (RadioStation) -> Unit,
) {
    AppleRadioCarousel(
        rows = stations.map { station ->
            AppleRadioRow(
                id = station.id,
                name = station.name,
                isLive = station.isLive,
                subtitle = station.subtitle,
                artworkUrl = station.artworkUrl,
                provider = station.provider,
                browseId = station.browseId,
            )
        },
        onRowClick = { row ->
            // The row is the station itself, so the station's own click is
            // what happens: a playlist opens as a page, a station plays.
            val original = stations.firstOrNull { it.id == row.id }
            if (original != null) onStationClick(original)
        },
    )
}

/**
 * A horizontal run of the page's own rows: the artwork, the name, and who
 * it is from.
 */
@Composable
private fun AppleRadioCarousel(
    rows: List<AppleRadioRow>,
    onRowClick: (AppleRadioRow) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(rows, key = { "apple-radio:carousel:${it.id}" }) { row ->
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .clickable { onRowClick(row) },
            ) {
                AsyncImage(
                    model = row.artworkUrl?.artworkAt(CARD_ART_PX),
                    contentDescription = row.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val under = row.provider ?: row.subtitle
                if (!under.isNullOrBlank()) {
                    Text(
                        text = under,
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
 * One station.
 *
 * The LIVE mark is Apple's own: a live station is a continuous stream, and
 * the mark says that the row plays here rather than being a page.
 */
@Composable
private fun AppleStationRow(
    station: RadioStation,
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
            model = station.artworkUrl?.artworkAt(ROW_ART_PX),
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
                text = station.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = station.subtitle
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (station.isLive) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE8342A))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.live),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
    }
}
