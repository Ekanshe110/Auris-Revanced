package com.music.bitchord

import com.music.bitchord.data.apple.AppleEditorialItem
import com.music.bitchord.data.apple.AppleEditorialShelf
import com.music.bitchord.data.apple.AppleSources
import com.music.bitchord.data.apple.toEditorialHero
import com.music.bitchord.data.apple.toHomeShelf
import com.music.bitchord.data.apple.toShelfItem
import com.music.bitchord.data.sources.addon.AddonManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AppleEditorialMappingTest {
    private val handle = AppleSources.Handle(
        configId = "apple-test",
        baseUrl = "http://localhost",
        manifest = AddonManifest(),
    )

    @Test
    fun `Apple resource plurals retain every kind of genre shelf`() {
        val shelves = listOf(
            "Playlists" to "playlists",
            "Best New Songs" to "songs",
            "New Releases" to "albums",
            "Stations" to "stations",
            "Artists We Love" to "artists",
        ).map { (title, type) ->
            AppleEditorialShelf(
                title = title,
                resourceTypes = listOf(type),
                items = listOf(AppleEditorialItem(id = "id-$type", type = type, name = title)),
            )
        }

        val mapped = shelves.mapNotNull { it.toHomeShelf(handle) }

        assertEquals(shelves.map { it.title }, mapped.map { it.title })
        assertEquals(AppleSources.CPAGE_PREFIX + "id-playlists", mapped[0].items.single().browseId)
        assertNotNull(mapped[1].items.single().videoId)
        assertNull(mapped[1].items.single().browseId)
        assertEquals(AppleSources.ALBUM_PREFIX + "id-albums", mapped[2].items.single().browseId)
        assertEquals(AppleSources.STATION_PREFIX + "id-stations", mapped[3].items.single().browseId)
        assertEquals(AppleSources.ARTIST_PREFIX + "id-artists", mapped[4].items.single().browseId)
    }

    @Test
    fun `singular resource names remain valid for older addon responses`() {
        listOf(
            "song", "album", "playlist", "station", "artist", "curator",
            "apple-curator", "music-video", "uploaded-video", "music-movie",
        ).forEach { type ->
            assertNotNull(
                type,
                AppleEditorialItem(id = "id", type = type, name = "Name").toShelfItem(handle),
            )
        }
    }

    @Test
    fun `video shelves keep Apple destinations instead of fake song ids`() {
        listOf("music-videos", "uploaded-videos", "music-movies").forEach { type ->
            val url = "https://music.apple.com/in/video/$type/123"
            val item = AppleEditorialItem(
                id = "123",
                type = type,
                name = "Video",
                artistName = "Artist",
                url = url,
            ).toShelfItem(handle)

            assertNotNull(type, item)
            assertNull(item?.videoId)
            assertNull(item?.browseId)
            assertEquals(url, item?.externalUrl)
        }
    }

    @Test
    fun `uploaded video without public URL stays visible but has no fake destination`() {
        val item = AppleEditorialItem(
            id = "123",
            type = "uploaded-videos",
            name = "Apple interview",
            url = "",
        ).toShelfItem(handle)

        assertNotNull(item)
        assertNull(item?.videoId)
        assertNull(item?.browseId)
        assertNull(item?.externalUrl)
    }

    @Test
    fun `curator kinds use their distinct Apple page routes`() {
        val ordinary = AppleEditorialItem(id = "10", type = "curators", name = "Curator")
            .toShelfItem(handle)
        val editorial = AppleEditorialItem(id = "20", type = "apple-curators", name = "Editorial")
            .toShelfItem(handle)

        assertEquals(AppleSources.CURATOR_PREFIX + "10", ordinary?.browseId)
        assertEquals(AppleSources.GENRE_PREFIX + "20", editorial?.browseId)
    }

    @Test
    fun `mixed shelves preserve every item in Apple order`() {
        val types = listOf("songs", "music-videos", "uploaded-videos", "playlists", "curators")
        val shelf = AppleEditorialShelf(
            title = "Mixed",
            resourceTypes = types,
            items = types.mapIndexed { index, type ->
                AppleEditorialItem(id = "$index", type = type, name = "Item $index")
            },
        )

        val mapped = shelf.toHomeShelf(handle)

        assertEquals(types.size, mapped?.items?.size)
        assertEquals(shelf.items.map { it.name }, mapped?.items?.map { it.title })
    }

    @Test
    fun `feature cards retain metadata for every resource kind`() {
        listOf("playlists", "music-videos", "curators", "apple-curators").forEach { type ->
            val hero = AppleEditorialItem(
                id = "id",
                type = type,
                name = "Feature",
                badge = "Apple badge",
                description = "Apple editorial note",
            ).toEditorialHero(handle)

            assertNotNull(type, hero)
            assertEquals("Apple badge", hero?.badge)
            assertEquals("Apple editorial note", hero?.description)
        }
    }

    @Test
    fun `mapping never truncates a genre shelf`() {
        val shelf = AppleEditorialShelf(
            title = "Best New Songs",
            resourceTypes = listOf("songs"),
            items = List(37) { index ->
                AppleEditorialItem(id = "$index", type = "songs", name = "Song $index")
            },
        )

        assertEquals(37, shelf.toHomeShelf(handle)?.items?.size)
    }
}
