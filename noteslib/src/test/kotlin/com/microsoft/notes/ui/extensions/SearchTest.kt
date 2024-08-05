package com.microsoft.notes.ui.extensions

import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.generateLocalId
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Paragraph
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class SearchTest {
    companion object {
        val ONE = createTestNote(
            "Villa Park",
            "Villa Park is a football stadium in Aston, Birmingham, England, with a seating capacity of " +
                "42,682. It has been the home of Aston Villa Football Club since 1897. The ground is " +
                "less than a mile from both Witton and Aston railway stations and has hosted sixteen " +
                "England internationals at senior level, the first in 1899 and the most recent in 2005. " +
                "Villa Park has hosted 55 FA Cup semi-finals, more than any other stadium.",
            "In 1897, Aston Villa moved into the Aston Lower Grounds, a sports ground in a Victorian " +
                "amusement park in the former grounds of Aston Hall, a Jacobean stately home. The " +
                "stadium has gone through various stages of renovation and development, resulting in" +
                " the current stand configuration of the Holte End, Trinity Road Stand, North Stand and " +
                "Doug Ellis Stand. The club has initial planning permission to redevelop the North " +
                "Stand, which will increase the capacity of Villa Park from 42,682 to about 50,000.",
            "Before 1914, a cycling track ran around the perimeter of the pitch where regular cycling " +
                "meetings were hosted as well as athletic events. Aside from football-related uses, the " +
                "stadium has seen various concerts staged along with other sporting events including " +
                "boxing matches and international rugby league and rugby union matches. In 1999, the last" +
                " ever final of the UEFA Cup Winners' Cup took place at Villa Park. Villa Park also " +
                "hosted the 2012 FA Community Shield, as Wembley Stadium was in use for the final of the " +
                "Olympic football tournament.[3]",
            color = Color.YELLOW
        )

        val TWO = createTestNote(
            "King Kalākaua's world tour",
            "The 1881 world tour of King Kalākaua of the Kingdom of Hawaii was his attempt to save the " +
                "Hawaiian culture and population from extinction through the importation of a labor " +
                "force from Asia-Pacific nations. His efforts brought the small island nation to the " +
                "attention of world leaders, but sparked rumors that the kingdom was for sale. " +
                "In Hawaii there were critics who believed the labor negotiations were just his excuse " +
                "to see the world. The 281-day trip gave him the distinction of being the first monarch " +
                "to circumnavigate the globe, just as his 1874 travels had made him the first reigning " +
                "monarch to visit America and the first honoree of a state dinner at the White House.",
            "Kalākaua met with heads of state in Asia, the Mideast and Europe, to encourage an influx of " +
                "sugar plantation labor in family groups, as well as unmarried women as potential brides " +
                "for Hawaii's existing contract laborers. While in Asia, he tried to forestall American " +
                "ambitions by offering a plan to Emperor Meiji for putting Hawaii under the protection of" +
                " the Empire of Japan with an arranged marriage between his niece Kaʻiulani and a " +
                "Japanese prince. On his visit to Portugal, he negotiated a treaty of friendship and " +
                "commerce with Hawaii that would provide a legal framework for the emigration of " +
                "Portuguese laborers to Hawaii. The King had an audience in Rome with Pope Leo XIII and " +
                "met with many of the crowned heads of Europe. Britain's Queen Victoria and the splendor " +
                "of her royal life impressed him more than any other monarchy; having been greatly " +
                "affected by the ornate trappings of European sovereigns, he would soon have Hawaii's " +
                "monarchy mirror that grandeur.",
            color = Color.GREEN
        )

        val THREE = createTestNote("Simple note", color = Color.PINK)

        val FOUR = createTestNote(
            "Courtney Love",
            "Courtney Michelle Love (née Harrison; born July 9, 1964) is an American singer, actress, writer," +
                " and visual artist. Prolific in the punk and grunge scenes of the 1990s, Love has " +
                "enjoyed a career that spans four decades. She rose to prominence as the frontwoman of " +
                "the alternative rock band Hole, which she formed in 1989. Love has drawn public " +
                "attention for her uninhibited live performances and confrontational lyrics, as well as " +
                "her highly publicized personal life following her marriage to Kurt Cobain.",
            color = Color.GREEN
        )

        val FIVE = createTestNote("Paragraph only", color = Color.PINK)

        val SIX = createTestNote(
            "Media Test", color = Color.CHARCOAL,
            media = Media(localUrl = "", mimeType = "", altText = "Alt Text", imageDimensions = null)
        )

        val allNotes = listOf(ONE, TWO, THREE, FOUR, FIVE, SIX)

        val ONE_SAMN = createTestSamsungNote("still running", "always remember to never be anyway feel free no way")

        val TWO_SAMN = createTestSamsungNote(
            "another one", "and another one and another one and it don't ever stop",
            "nah not yet"
        )

        val allSamsungNotes = listOf(ONE_SAMN, TWO_SAMN)

        private fun createTestNote(vararg paragraphs: String, color: Color, media: Media? = null): Note {
            val mediaList = mutableListOf<Media>()
            media?.let { mediaList.add(media) }
            return Note(
                document = Document(
                    blocks = paragraphs.map {
                        Paragraph(localId = generateLocalId(), content = Content(it, spans = emptyList()))
                    }
                ),
                color = color,
                media = mediaList
            )
        }

        private fun createTestSamsungNote(title: String, vararg paragraphs: String) =
            createTestNote(*paragraphs, color = Color.YELLOW).copy(
                createdByApp = SAMSUNG_NOTES_APP_NAME,
                title = title
            )
    }

    @Test
    fun `should filter by single search term`() {
        val query = "the"
        val result = allNotes.search(query, null)

        assertThat(result, iz(listOf(ONE, TWO, FOUR)))
    }

    @Test
    fun `should filter by two search terms`() {
        val query = "prolific singer"
        val result = allNotes.search(query, null)

        assertThat(result, iz(listOf(FOUR)))
    }

    @Test
    fun `should filter to nothing if unknown term`() {
        val query = "qjbkmjqkgqc"
        val result = allNotes.search(query, null)

        assertThat(result, iz(emptyList()))
    }

    @Test
    fun `should filter to sub word match`() {
        val query = "nation"
        val result = allNotes.search(query, null)

        assertThat(result, iz(listOf(ONE, TWO)))
    }

    @Test
    fun `should filter to notes that match all terms`() {
        val query = "19 state"
        val result = allNotes.search(query, null)

        assertThat(result, iz(listOf(ONE)))
    }

    @Test
    fun `should match case insensitive`() {
        val query = "WORLD"
        val result = allNotes.search(query, null)

        assertThat(result, iz(listOf(TWO)))
    }

    @Test
    fun `should match body only`() {
        val query = "note"
        val result = allNotes.search(query, null)

        assertThat(result, iz(listOf(THREE)))
    }

    @Test
    fun `should match nothing with empty list`() {
        val query = "test"
        val result = emptyList<Note>().search(query, null)

        assertThat(result, iz(emptyList()))
    }

    @Test
    fun `should match all notes with single letter`() {
        val query = "o"
        val result = allNotes.search(query, null)

        assertThat(result, iz(listOf(ONE, TWO, THREE, FOUR, FIVE)))
    }

    @Test
    fun `should match all notes with empty query`() {
        val query = ""
        val result = allNotes.search(query, null)

        assertThat(result, iz(allNotes))
    }

    @Test
    fun `should match all notes with whitespace query`() {
        val query = " \t"
        val result = allNotes.search(query, null)

        assertThat(result, iz(allNotes))
    }

    @Test
    fun `should match all notes with color`() {
        val queryColor = Color.PINK
        val result = allNotes.search("", queryColor)
        assertThat(result, iz(listOf(THREE, FIVE)))
    }

    @Test
    fun `should match all notes with color and query`() {
        val queryColor = Color.PINK
        val query = "note"
        val result = allNotes.search(query, queryColor)
        assertThat(result, iz(listOf(THREE)))
    }

    @Test
    fun `should match all notes with alt text`() {
        val query = "Alt Text"
        val result = allNotes.search(query, null)
        assertThat(result, iz(listOf(SIX)))
    }

    // Samsung Note test
    @Test
    fun `samsung note result found in document`() {
        val query = "stop"
        val result = allSamsungNotes.search(query, null)
        assertThat(result, iz(listOf(TWO_SAMN)))
    }

    @Test
    fun `samsung note result found in title`() {
        val query = "runnin"
        val result = allSamsungNotes.search(query, null)
        assertThat(result, iz(listOf(ONE_SAMN)))
    }
}
