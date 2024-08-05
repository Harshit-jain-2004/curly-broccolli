package com.microsoft.notes.threeWayMerge

import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.scheme.Block
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
import com.microsoft.notes.richtext.scheme.asMedia
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class DiffTest {

    val PREFIX_OFFSET_TEST_DATA = listOf<Triple<String, String, Int>>(
        Triple("foo", "fot", 2),
        Triple("bar", "foo", 0),
        Triple("a", "apple", 1),
        Triple("bad", "bat", 2),
        Triple("", "foo", 0),
        Triple("foo", "", 0)
    )

    @Test
    fun should_test_all_prefix_offset() {
        PREFIX_OFFSET_TEST_DATA.forEach {
            with(it) {
                assertThat(prefixOffset(first, second), iz(third))
            }
        }
    }

    val SUFIX_OFFSET_TEST_DATA = listOf<Triple<String, String, Pair<Int, Int>>>(
        Triple("book", "took", Pair(1, 1)),
        Triple("book", "book", Pair(0, 0)),
        Triple("b", "ab", Pair(0, 1)),
        Triple("ab", "b", Pair(1, 0)),
        Triple("book", "a book", Pair(0, 2)),
        Triple("a boy", "a toy", Pair(3, 3)),
        Triple("a foot at the table", "a book at the table", Pair(6, 6)),
        Triple("a horse flies", "a cow flies", Pair(7, 5)),
        Triple("", "foo", Pair(0, 3)),
        Triple("a book", "there was one book", Pair(1, 13)),
        Triple("foo", "", Pair(3, 0))
    )

    @Test
    fun should_test_all_suffix_offset() {
        SUFIX_OFFSET_TEST_DATA.forEach {
            with(it) {
                assertThat(suffixOffset(first, second, 0), iz(third))
            }
        }
    }

    val CHANGED_RUNS_DATA_TEST =
        listOf<Triple<String, String, Pair<Range, Range>>>(
            Triple(
                "a big book", "a small book",
                Pair(
                    Range(2, 5),
                    Range(2, 7)
                )
            ),
            Triple(
                "a book", "the book",
                Pair(
                    Range(0, 1),
                    Range(0, 3)
                )
            ),
            Triple(
                "the dog", "the cat",
                Pair(
                    Range(4, 7),
                    Range(4, 7)
                )
            ),
            Triple(
                "", "foo",
                Pair(
                    Range(0, 0),
                    Range(0, 3)
                )
            ),
            Triple(
                "book", "tool",
                Pair(
                    Range(0, 4),
                    Range(0, 4)
                )
            ),
            Triple(
                "foo", "",
                Pair(
                    Range(0, 3),
                    Range(0, 0)
                )
            )
        )

    @Test
    fun should_test_all_changed_ranges() {
        CHANGED_RUNS_DATA_TEST.forEach {
            with(it) {
                assertThat(changedRanges(first, second), iz(third))
            }
        }
    }

    val TO_DELETE_DATA_TEST =
        listOf<Triple<String, String, List<Int>>>(
            Triple("a big book", "a small book", listOf(2, 3, 4)),
            Triple("a small book", "a big book", listOf(2, 3, 4, 5, 6)),
            Triple("book", "tool", listOf(0, 3)),
            Triple("book", "book", emptyList()),
            Triple("book", "", listOf(0, 1, 2, 3)),
            Triple("", "book", emptyList())
        )

    @Test
    fun should_test_to_delete() {
        TO_DELETE_DATA_TEST.forEach {
            with(it) {
                val changedRanges = changedRanges(first, second)
                assertThat(toDeleteIndices(first, second, changedRanges), iz(third))
            }
        }
    }

    val TO_INSERT_DATA_TEST =
        listOf<Triple<String, String, List<CharInsert>>>(
            Triple(
                "a big book", "a small book",
                listOf(
                    CharInsert(2, 's'),
                    CharInsert(3, 'm'),
                    CharInsert(4, 'a'),
                    CharInsert(5, 'l'),
                    CharInsert(6, 'l')
                )
            ),
            Triple(
                "a small book", "a big book",
                listOf(
                    CharInsert(2, 'b'),
                    CharInsert(3, 'i'),
                    CharInsert(4, 'g')
                )
            ),
            Triple(
                "book", "tool",
                listOf(
                    CharInsert(0, 't'),
                    CharInsert(3, 'l')
                )
            ),
            Triple(
                "", "tool",
                listOf(
                    CharInsert(0, 't'),
                    CharInsert(1, 'o'),
                    CharInsert(2, 'o'),
                    CharInsert(3, 'l')
                )
            ),
            Triple("tool", "", emptyList()),
            Triple("", "", emptyList()),
            Triple(
                "立ネウラト擬禁海ロ", "立ネ海ウラト擬禁海ロ",
                listOf(
                    CharInsert(2, '海')
                )
            )
        )

    @Test
    fun should_insert_characters() {
        TO_INSERT_DATA_TEST.forEach {
            with(it) {
                val changedRanges = changedRanges(first, second)
                assertThat(toCharInsert(first, second, changedRanges), iz(third))
            }
        }
    }

    val TEST_BLOCK_ID = "blockId"
    val TEXT_INSERTS_DATA_TEST = listOf<Triple<String, String, List<BlockTextInsertion>>>(
        Triple(
            "a big book", "a small book",
            listOf(
                BlockTextInsertion(TEST_BLOCK_ID, "small", 2)
            )
        ),
        Triple(
            "book", "tool",
            listOf(
                BlockTextInsertion(TEST_BLOCK_ID, "t", 0),
                BlockTextInsertion(TEST_BLOCK_ID, "l", 3)
            )
        ),
        Triple(
            "", "a small book",
            listOf(
                BlockTextInsertion(TEST_BLOCK_ID, "a small book", 0)
            )
        ),
        Triple("a small book", "", emptyList())
    )

    @Test
    fun should_insert_text() {
        TEXT_INSERTS_DATA_TEST.forEach {
            with(it) {
                val changedRanges = changedRanges(first, second)
                assertThat(
                    textInserts(
                        first, second, changedRanges,
                        TEST_BLOCK_ID
                    ),
                    iz(third)
                )
            }
        }
    }

    val TEXT_DELETES_DATA_TEST = listOf<Triple<String, String, List<BlockTextDeletion>>>(
        Triple(
            "a big book", "a small book",
            listOf(
                BlockTextDeletion(TEST_BLOCK_ID, 2, 4)
            )
        ),
        Triple(
            "a small book", "a big book",
            listOf(
                BlockTextDeletion(TEST_BLOCK_ID, 2, 6)
            )
        ),
        Triple(
            "book", "tool",
            listOf(
                BlockTextDeletion(TEST_BLOCK_ID, 0, 0),
                BlockTextDeletion(TEST_BLOCK_ID, 3, 3)
            )
        ),
        Triple("", "tool", emptyList()),
        Triple(
            "book", "",
            listOf(
                BlockTextDeletion(TEST_BLOCK_ID, 0, 3)
            )
        )
    )

    @Test
    fun should_delete_text() {
        TEXT_DELETES_DATA_TEST.forEach {
            with(it) {
                val changedRanges = changedRanges(first, second)
                assertThat(
                    textDeletes(
                        first, second, changedRanges,
                        TEST_BLOCK_ID
                    ),
                    iz(third)
                )
            }
        }
    }

    val DELETE_SPANS_DATA_TEST = listOf<Triple<List<Span>, List<Span>, List<SpanDeletion>>>(
        Triple(
            listOf(Span(SpanStyle.BOLD, 0, 1, 0)),
            listOf(Span(SpanStyle.BOLD, 0, 1, 0)),
            emptyList()
        ),
        Triple(
            listOf(Span(SpanStyle.BOLD, 0, 1, 0)),
            listOf(Span(SpanStyle.BOLD, 2, 5, 0)),
            listOf(
                SpanDeletion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.BOLD, 0, 1, 0)
                )
            )
        ),
        Triple(
            listOf(Span(SpanStyle.BOLD_ITALIC, 0, 1, 0), Span(SpanStyle.UNDERLINE, 0, 1, 0)),
            listOf(Span(SpanStyle.UNDERLINE, 0, 1, 0)),
            listOf(
                SpanDeletion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.BOLD_ITALIC, 0, 1, 0)
                )
            )
        ),
        Triple(
            emptyList(),
            listOf(Span(SpanStyle.UNDERLINE, 0, 1, 0)),
            emptyList()
        ),
        Triple(
            listOf(Span(SpanStyle.UNDERLINE, 0, 1, 0)),
            emptyList(),
            listOf(
                SpanDeletion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.UNDERLINE, 0, 1, 0)
                )
            )
        ),
        Triple(
            listOf(Span(SpanStyle.BOLD, 0, 1, 0)),
            listOf(Span(SpanStyle.DEFAULT, 0, 1, 0)),
            listOf(
                SpanDeletion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.BOLD, 0, 1, 0)
                )
            )
        )
    )

    @Test
    fun should_delete_spans() {
        DELETE_SPANS_DATA_TEST.forEach {
            with(it) {
                assertThat(spanDeletes(first, second, TEST_BLOCK_ID), iz(third))
            }
        }
    }

    val INSERT_SPANS_DATA_TEST = listOf<Triple<List<Span>, List<Span>, List<SpanInsertion>>>(
        Triple(
            listOf(Span(SpanStyle.BOLD, 0, 1, 0)),
            listOf(Span(SpanStyle.BOLD, 0, 1, 0)),
            emptyList()
        ),
        Triple(
            listOf(Span(SpanStyle.BOLD, 0, 1, 0)),
            listOf(Span(SpanStyle.BOLD, 0, 1, 0), Span(SpanStyle.ITALIC, 0, 1, 0)),
            listOf(
                SpanInsertion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.ITALIC, 0, 1, 0)
                )
            )
        ),
        Triple(
            listOf(Span(SpanStyle.BOLD, 0, 1, 0), Span(SpanStyle.ITALIC, 0, 1, 0)),
            listOf(Span(SpanStyle.BOLD, 0, 1, 0)),
            emptyList()
        ),
        Triple(
            listOf(Span(SpanStyle.BOLD, 0, 1, 0), Span(SpanStyle.ITALIC, 0, 1, 0)),
            emptyList(),
            emptyList()
        ),
        Triple(
            listOf(Span(SpanStyle.BOLD, 0, 1, 0)),
            listOf(Span(SpanStyle.BOLD, 0, 5, 0), Span(SpanStyle.ITALIC, 0, 1, 0)),
            listOf(
                SpanInsertion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.BOLD, 0, 5, 0)
                ),
                SpanInsertion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.ITALIC, 0, 1, 0)
                )
            )
        ),
        Triple(
            listOf(Span(SpanStyle.BOLD, 0, 1, 0)),
            listOf(Span(SpanStyle.BOLD, 3, 5, 0), Span(SpanStyle.ITALIC, 5, 10, 0)),
            listOf(
                SpanInsertion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.BOLD, 3, 5, 0)
                ),
                SpanInsertion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.ITALIC, 5, 10, 0)
                )
            )
        ),
        Triple(
            listOf(Span(SpanStyle.BOLD, 0, 1, 0)),
            listOf(Span(SpanStyle.DEFAULT, 0, 1, 0)),
            listOf(
                SpanInsertion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.DEFAULT, 0, 1, 0)
                )
            )
        )
    )

    @Test
    fun should_insert_spans() {
        INSERT_SPANS_DATA_TEST.forEach {
            with(it) {
                assertThat(spanInserts(first, second, TEST_BLOCK_ID), iz(third))
            }
        }
    }

    val CONTENT_DIFF_DATA_TEST = listOf<Triple<Paragraph, Paragraph, List<BlockDiff>>>(
        Triple(
            Paragraph(
                localId = TEST_BLOCK_ID,
                style = ParagraphStyle(),
                content = Content(
                    text = "book",
                    spans = listOf(Span(SpanStyle.BOLD, 1, 2, 0))
                )
            ),
            Paragraph(
                localId = TEST_BLOCK_ID, style = ParagraphStyle(),
                content = Content(
                    text = "look",
                    spans = listOf(Span(SpanStyle.ITALIC, 1, 2, 0))
                )
            ),
            listOf(
                BlockTextDeletion(TEST_BLOCK_ID, 0, 0),
                BlockTextInsertion(TEST_BLOCK_ID, "l", 0),
                SpanDeletion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.BOLD, 1, 2, 0)
                ),
                SpanInsertion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.ITALIC, 1, 2, 0)
                )
            )
        )
    )

    @Test
    fun should_diff_content() {
        CONTENT_DIFF_DATA_TEST.forEach {
            with(it) {
                val result = contentDiffs(first, second)
                assertThat(result.size, iz(third.size))
                result.forEach {
                    assertTrue(third.contains(it))
                }
            }
        }
    }

    val BLOCK_DIFF_DATA_TEST = listOf<Triple<Block, Block, List<Diff>>>(
        Triple(
            Paragraph(
                localId = TEST_BLOCK_ID,
                style = ParagraphStyle(),
                content = Content(
                    text = "book",
                    spans = listOf(Span(SpanStyle.BOLD, 1, 2, 0))
                )
            ),
            Paragraph(
                localId = TEST_BLOCK_ID, style = ParagraphStyle(unorderedList = true),
                content = Content(
                    text = "look",
                    spans = listOf(Span(SpanStyle.ITALIC, 1, 2, 0))
                )
            ),
            listOf(
                UnorderedListInsertion(TEST_BLOCK_ID),
                BlockTextDeletion(TEST_BLOCK_ID, 0, 0),
                BlockTextInsertion(TEST_BLOCK_ID, "l", 0),
                SpanInsertion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.ITALIC, 1, 2, 0)
                ),
                SpanDeletion(
                    TEST_BLOCK_ID,
                    Span(SpanStyle.BOLD, 1, 2, 0)
                )
            )
        ),
        Triple(
            InlineMedia(localId = TEST_BLOCK_ID, localUrl = "/media1"),
            InlineMedia(localId = TEST_BLOCK_ID, localUrl = "/media2", remoteUrl = "/remote1"),
            listOf(
                BlockUpdate(
                    InlineMedia(localId = TEST_BLOCK_ID, localUrl = "/media2", remoteUrl = "/remote1")
                )
            )
        )
    )

    @Test
    fun should_diff_block() {
        BLOCK_DIFF_DATA_TEST.forEach {
            with(it) {
                val result = blockDiffs(first, second)
                assertThat(result.size, iz(third.size))
                result.forEach {
                    assertTrue(third.contains(it))
                }
            }
        }
    }

    val NOTE_WITH_YELLOW_COLOR = Note(
        color = Color.YELLOW,
        document = Document(listOf(Paragraph(content = Content(text = "the body"))))
    )

    @Test
    fun should_get_color_diff_from_Notes() {
        val noteChanged = NOTE_WITH_YELLOW_COLOR.copy(color = Color.BLUE)
        val diffs = diff(NOTE_WITH_YELLOW_COLOR, noteChanged)
        assertThat(diffs.size, iz(1))
        assertThat(
            diffs.component1(),
            iz(
                ColorUpdate(
                    Color.BLUE
                ) as Diff
            )
        )
    }

    val NOTE_WITH_THREE_PARAGRAPHS = Note(
        document = Document(
            listOf(
                Paragraph(content = Content(text = "one")),
                Paragraph(content = Content(text = "two")),
                Paragraph(content = Content(text = "three"))
            )
        )
    )

    @Test
    fun should_get_insertion_block_diff_when_adding_a_new_paragraph_in_the_target() {
        val newParagraph = Paragraph(localId = "new", content = Content(text = "one point five"))
        val newParagraphList = NOTE_WITH_THREE_PARAGRAPHS.document.blocks.toMutableList()
        newParagraphList.add(1, newParagraph)
        val newDocument = NOTE_WITH_THREE_PARAGRAPHS.document.copy(blocks = newParagraphList)
        val noteChanged = NOTE_WITH_THREE_PARAGRAPHS.copy(document = newDocument)
        val diffs = diff(NOTE_WITH_THREE_PARAGRAPHS, noteChanged)
        assertThat(diffs.size, iz(1))
        assertThat(
            diffs.component1(),
            iz(
                BlockInsertion(
                    block = newParagraph,
                    index = 1
                ) as Diff
            )
        )
    }

    @Test
    fun should_get_deletion_block_diff_when_adding_a_new_paragraph_in_the_base() {
        val newParagraph = Paragraph(localId = "new", content = Content(text = "one point five"))
        val newParagraphList = NOTE_WITH_THREE_PARAGRAPHS.document.blocks.toMutableList()
        newParagraphList.add(1, newParagraph)
        val newDocument = NOTE_WITH_THREE_PARAGRAPHS.document.copy(blocks = newParagraphList)
        val noteChanged = NOTE_WITH_THREE_PARAGRAPHS.copy(document = newDocument)
        val diffs = diff(noteChanged, NOTE_WITH_THREE_PARAGRAPHS)
        assertThat(diffs.size, iz(1))
        assertThat(
            diffs.component1(),
            iz(
                BlockDeletion(
                    blockId = "new"
                ) as Diff
            )
        )
    }

    @Test
    fun should_get_unordered_deletion_diff() {
        val paragraph = Paragraph(localId = "id1", content = Content("A list item"))
        val note = Note(document = Document(listOf(paragraph)))
        val newParagraph = paragraph.copy(style = ParagraphStyle(true))
        val newParagraphList = note.document.blocks.toMutableList()
        newParagraphList[0] = newParagraph
        val noteWithBulletList = note.copy(
            document = note.document.copy(
                blocks = newParagraphList.toList()
            )
        )
        val diffs = diff(noteWithBulletList, note)
        assertThat(diffs.size, iz(1))
        assertThat(
            diffs.component1(),
            iz(
                UnorderedListDeletion(
                    paragraph.localId
                ) as Diff
            )
        )
    }

    @Test
    fun should_get_unordered_insertion_diff() {
        val paragraph = Paragraph(localId = "id1", content = Content("A list item"))
        val note = Note(document = Document(listOf(paragraph)))
        val newParagraph = paragraph.copy(style = ParagraphStyle(true))
        val newParagraphList = note.document.blocks.toMutableList()
        newParagraphList[0] = newParagraph
        val noteWithBulletPoint = note.copy(
            document = note.document.copy(
                blocks = newParagraphList.toList()
            )
        )
        val diffs = diff(note, noteWithBulletPoint)
        assertThat(diffs.size, iz(1))
        assertThat(
            diffs.component1(),
            iz(
                UnorderedListInsertion(
                    paragraph.localId
                ) as Diff
            )
        )
    }

    @Test
    fun should_get_insert_block_text_as_diff() {
        val paragraph = Paragraph(localId = "id1", content = Content("you are likely to be eaten"))
        val note = Note(document = Document(listOf(paragraph)))
        val newContent = paragraph.content.copy(text = "you are likely to be eaten by a grue")
        val newParagraph = paragraph.copy(content = newContent)
        val newParagraphList = note.document.blocks.toMutableList()
        newParagraphList[0] = newParagraph
        val noteWithNewContent = note.copy(
            document = note.document.copy(
                blocks = newParagraphList.toList()
            )
        )
        val diffs = diff(note, noteWithNewContent)
        assertThat(diffs.size, iz(1))
        assertThat(
            diffs.component1(),
            iz(
                BlockTextInsertion(
                    paragraph.localId, " by a grue",
                    26
                ) as Diff
            )
        )
    }

    @Test
    fun should_get_update_diff_when_media_blocks() {
        val mediaWithoutLocalUrl = InlineMedia(
            localId = "336e7e23-e33d-4080-b6ab-cffcdfc2089d",
            localUrl = null,
            remoteUrl = "/api/beta/me/notes/AAMkAGQ2OWI3NzJlLWM3NTUtNDVjMC1iNTYxLTI0OGU2NDA1YzNhOQBGAAAAAAAQwKZl23IIRJZcbv25jM1zBwB-8RDfQ0W6RLHaZ8V3cRaWAAAATicUAABQOVMLvJ3lTKGpodgq9AREAAD-Wis5AAA=/media/AAMkAGQ2OWI3NzJlLWM3NTUtNDVjMC1iNTYxLTI0OGU2NDA1YzNhOQBGAAAAAAAQwKZl23IIRJZcbv25jM1zBwB-8RDfQ0W6RLHaZ8V3cRaWAAAATicUAABQOVMLvJ3lTKGpodgq9AREAAD-Wis5AAABEgAQAO0-xvKE3TVNqrI7bM0R5AI=",
            mimeType = "image/jpeg"
        )
        val mediaWithLocalAndRemoteUrl = InlineMedia(
            localId = "336e7e23-e33d-4080-b6ab-cffcdfc2089d",
            localUrl = "/data/user/0/com.microsoft.notes.dev/files/media_336e7e23-e33d-4080-b6ab-cffcdfc2089d.jpg",
            remoteUrl = "/api/beta/me/notes/AAMkAGQ2OWI3NzJlLWM3NTUtNDVjMC1iNTYxLTI0OGU2NDA1YzNhOQBGAAAAAAAQwKZl23IIRJZcbv25jM1zBwB-8RDfQ0W6RLHaZ8V3cRaWAAAATicUAABQOVMLvJ3lTKGpodgq9AREAAD-Wis5AAA=/media/AAMkAGQ2OWI3NzJlLWM3NTUtNDVjMC1iNTYxLTI0OGU2NDA1YzNhOQBGAAAAAAAQwKZl23IIRJZcbv25jM1zBwB-8RDfQ0W6RLHaZ8V3cRaWAAAATicUAABQOVMLvJ3lTKGpodgq9AREAAD-Wis5AAABEgAQAO0-xvKE3TVNqrI7bM0R5AI=",
            mimeType = "image/jpeg"
        )
        val note1 = Note(document = Document(listOf(mediaWithoutLocalUrl)))
        val note2 = note1.copy(
            document = note1.document.copy(
                blocks = listOf(mediaWithLocalAndRemoteUrl)
            )
        )
        val diffs = diff(note1, note2)
        assertThat(diffs.size, iz(1))
        assertThat(diffs.component1() is BlockUpdate, iz(true))
        assertThat((diffs.component1() as BlockUpdate).block is InlineMedia, iz(true))
        with((diffs.component1() as BlockUpdate).block.asMedia()) {
            assertThat(this, iz(mediaWithLocalAndRemoteUrl))
        }
    }

    @Test
    fun should_map_by_block_id() {
        val block0 = Paragraph(localId = "block0", content = Content(text = "text block0"))
        val block1 = Paragraph(localId = "block1", content = Content(text = "text block1"))
        val blockDeletion = BlockDeletion(blockId = "block1")
        val blockInsertion = BlockInsertion(block = block0, index = 1)
        val blockUpdate = BlockUpdate(block = block1)
        val colorUpdate = ColorUpdate(color = Color.BLUE)
        val diffs = mutableListOf(blockDeletion, blockInsertion, blockUpdate, colorUpdate)

        val mapResult = diffs.toMapByBlockId()
        assertThat(mapResult.isNotEmpty(), iz(true))
        assertThat(mapResult.size, iz(2))

        val map0 = mapResult["block0"]
        assertThat(map0, iz(not(nullValue())))
        assertThat(map0!!.size, iz(1))
        assertThat(map0.component1() as BlockInsertion, iz(blockInsertion))

        val map1 = mapResult["block1"]
        assertThat(map1, iz(not(nullValue())))
        assertThat(map1!!.size, iz(2))
        assertThat(map1.component1() as BlockDeletion, iz(blockDeletion))
        assertThat(map1.component2() as BlockUpdate, iz(blockUpdate))
    }
}
