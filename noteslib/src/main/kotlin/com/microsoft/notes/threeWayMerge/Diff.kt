package com.microsoft.notes.threeWayMerge

import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.scheme.Block
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.threeWayMerge.diff.MediaDeletion
import com.microsoft.notes.threeWayMerge.diff.MediaInsertion
import com.microsoft.notes.threeWayMerge.diff.idToMediaMap
import com.microsoft.notes.threeWayMerge.diff.mediaDiffs

data class Range(val start: Int, val end: Int)
data class CharInsert(val index: Int, val character: Char)

abstract class Diff
sealed class BlockDiff : Diff() {
    abstract val blockId: String
}

data class BlockDeletion(override val blockId: String) : BlockDiff()
data class BlockInsertion(val block: Block, val index: Int) : Diff()
data class BlockUpdate(val block: Block) : Diff()

sealed class TextDiffOperation : BlockDiff()
data class BlockTextDeletion(override val blockId: String, val start: Int, val end: Int) : TextDiffOperation()
data class BlockTextInsertion(override val blockId: String, val text: String, val index: Int) : TextDiffOperation()

data class SpanDeletion(override val blockId: String, val span: Span) : BlockDiff()
data class SpanInsertion(override val blockId: String, val span: Span) : BlockDiff()

sealed class UnorderedListOperation : BlockDiff()
data class UnorderedListDeletion(override val blockId: String) : UnorderedListOperation()
data class UnorderedListInsertion(override val blockId: String) : UnorderedListOperation()

sealed class RightToLeftOperation : BlockDiff()
data class RightToLeftDeletion(override val blockId: String) : RightToLeftOperation()
data class RightToLeftInsertion(override val blockId: String) : RightToLeftOperation()

data class ColorUpdate(val color: Color) : Diff()

/**
 * Given two strings [base] and [target] we return the index (inclusive) when the strings differ taking
 * base as the lead String.
 *
 * ie:  base: "abc"
 *      target: "ad"
 *      result: 1 (we have 'b' in the base and 'd' in the target)
 * @param base the first [String] to compare
 * @param target the second [String] to compare
 * @return the index [Int] that indicates where the two strings starting to be different taking base as the lead
 * [String].
 */
fun prefixOffset(base: String, target: String): Int {
    if (target.isEmpty()) return 0

    val targetLength = target.length
    base.forEachIndexed { i, character ->
        if (i >= targetLength || character != target[i]) {
            return i
        }
    }
    return base.length
}

/**
 * Given two strings [base] and [target] we return the index of where each String starting to be
 * equal to the other.
 *
 * ie:  base: "a book"
 *      target: "there was one book"
 *      prefixOffset: 0
 *      result: (1, 13) --> since the prefixOffset is 0 we start counting and comparing at index 0, so
 *      the string that is the same for both is " book"
 *
 *      base: "a book"
 *      target: "there was one book"
 *      prefixOffset: 3
 *      result: (3, 15) --> since the prefixOffset is 3 we start counting and comparing at index 3, so
 *      the String that is the same for both is "ook"
 *
 *      base: "a book"
 *      target: "there was one book"
 *      prefixOffset: 10
 *      result: (6, 18) --> since the prefixOffset is 10 we start counting and comparing at index 10, so
 *      we don't have any String that it common in both Strings, since index 10 is greater than the base lenght,
 *      so this result it says that there is no common String in both Strings.
 * @param base first [String] to compare
 * @param target second [String] to compare
 * @param prefixOffset prefix where we should start counting.
 * @result a [Pair]<[Int], [Int]> where we say where base and target start to be the same.
 */
fun suffixOffset(base: String, target: String, prefixOffset: Int): Pair<Int, Int> {
    if (target.isEmpty()) return Pair(base.length, 0)
    if (base.isEmpty()) return Pair(0, target.length)

    base.reversed().forEachIndexed { index, character ->
        val targetIndex = target.length - index - 1
        val baseIndex = base.length - index - 1
        if (targetIndex < 0) return Pair(baseIndex + 1, 0)
        if (targetIndex < prefixOffset || baseIndex < prefixOffset || character != target[targetIndex]) {
            return Pair(baseIndex + 1, targetIndex + 1)
        }
    }
    return Pair(0, target.length - base.length)
}

/**
 * This function returns two [Range] where we indicate where is the difference between the two given [String].
 * The first Range indicates where the difference is between base and target (start and ends) in the [base].
 * The second Range indicates where the difference is between the base and the target (start and ends) in the
 * [target].
 *
 * ie:
 *      base: "a big book"
 *      target: "a small book"
 *      result: Pair[(2, 5)(2, 7)] --> (2, 5) in the base (before 'b' and after 'g')
 *                                     (2, 7) in the target (before 's' and after 'l')
 *
 *      base: "the dog"
 *      target: "the cat"
 *      result: Pair[(4, 7), (4, 7)] --> (4, 7) in the base (before 'd' and after 'g')
 *                                       (2, 7) in the target (before 'c' and after 't')
 *
 * @param base the first [String] to compare
 * @param target the second [String] to compare
 * @result [Pair]<[Range], [Range]> the result, first Range belongs to [base], second to [target]
 */
fun changedRanges(base: String, target: String): Pair<Range, Range> {
    val prefixOffset = prefixOffset(base, target)
    val (baseSuffixOffset, targetSuffixOffset) = suffixOffset(
        base, target,
        prefixOffset
    )
    val baseRange = Range(prefixOffset, baseSuffixOffset)
    val targetRange = Range(prefixOffset, targetSuffixOffset)
    return Pair(baseRange, targetRange)
}

/**
 * Calculates the indices (positions) that we need to remove from the [base] so we could get the target from it.
 * So we get the indices (positions) that are different between the base and the target and we need to
 * delete from the base.
 *
 * ie:
 *      base: a big book -> changedRange: (2, 5)
 *      target: a small book -> changedRange: (2, 7)
 *      result: [2, 3, 4] -> we are going to delete 'big' from the base
 *
 *      base: book -> changedRange: (0, 3)
 *      target: foot -> changedRange: (0, 3)
 *      result: [0, 3] -> we are going to delete 'b' and 'k' from the base
 *
 * @param base the first [String] to compare and where we will get the indices to delete
 * @pram target the second [String] to compare that will be used to indicate the difference with the base
 * @result [List]<[Int]> a list of [Int] where every item is the index (position) from the [base] String that we
 * have to delete.
 */
fun toDeleteIndices(base: String, target: String, changedRanges: Pair<Range, Range>): List<Int> {
    val toDelete = mutableListOf<Int>()
    val (baseRange, targetRange) = changedRanges
    (baseRange.start until baseRange.end).filterTo(toDelete) { it >= targetRange.end || base[it] != target[it] }
    return toDelete
}

/**
 * Calculates the new content we have to add to the [base] so we can get [target].
 *
 * ie:
 *      base: "a big book"
 *      target: "a small book"
 *      result: List<CharInsert>[(2, 's'), (3, 'm'), (4, 'a'), (5, 'l'), (6, 'l')]
 *      so base becomes target
 *
 *      base: "book"
 *      target: "tool"
 *      result: List<CharInsert>[(0, 't'), (3, 'l')]
 *
 *  @param [base] String where difference between [target] will be applied
 *  @param [target] String that will be used to see which changes we have to apply on [base]
 *  @return [List]<[CharInsert]> a list containing the position-character items we have to apply on the [base] to
 *  get the [target]
 */
fun toCharInsert(base: String, target: String, changedRanges: Pair<Range, Range>): List<CharInsert> {
    val toInsert = mutableListOf<CharInsert>()
    val (baseRange, targetRange) = changedRanges
    (targetRange.start until targetRange.end)
        .filter { it >= baseRange.end || base[it] != target[it] }
        .mapTo(toInsert) { CharInsert(it, target[it]) }
    return toInsert
}

/**
 * Gets a [List]<[BlockTextInsertion]> where we say the text and position (start) where we should apply these list
 * in the [base] in order to get [target].
 *
 * ie:
 *      base: "a big book"
 *      target: "a small book"
 *      result: List<BlockTextInsertion>[("small", 2)] -> so in the position 2 of the base we should apply "small"
 *      in order to get [target]
 *
 *      base: "book"
 *      target: "tool"
 *      result: List<BlockTextInsertion>[("b", 0), ("l", 3)]  -> in the position 0 and 3 in the [base] applying
 *      respectively "b" and "l" we would get [target]
 *
 * @param base String where the result would be applied in.
 * @param target String that is the goal to have, so we want to have [target] given it and the [base]
 * @param changedRanges Difference between [base] and [target] expressed in ranges
 * @param blockId both base and target must to belong to the same [Block] so they have to have share same
 * [blockId].
 */
@Suppress("UnsafeCallOnNullableType")
fun textInserts(base: String, target: String, changedRanges: Pair<Range, Range>, blockId: String):
    List<BlockTextInsertion> {
    val inserts = toCharInsert(base, target, changedRanges)
    val diffs = mutableListOf<BlockTextInsertion>()
    var currentInsertionStart: Int? = null
    var lastInsertionPoint: Int? = null
    var insertionString = ""

    inserts.forEachIndexed { i, charInsert ->
        val (currentInsertionPoint, character) = charInsert
        currentInsertionStart = currentInsertionStart ?: currentInsertionPoint
        val oneLeftOfCurrentInsertionPoint = currentInsertionPoint - 1
        when (lastInsertionPoint) {
            null, oneLeftOfCurrentInsertionPoint -> {
                lastInsertionPoint = currentInsertionPoint
                insertionString += character
            }
            else -> {
                diffs.add(
                    BlockTextInsertion(
                        blockId, insertionString,
                        currentInsertionStart!!
                    )
                )
                lastInsertionPoint = currentInsertionPoint
                currentInsertionStart = lastInsertionPoint
                insertionString = character.toString()
            }
        }
        if (i == inserts.size - 1) {
            diffs.add(
                BlockTextInsertion(
                    blockId, insertionString,
                    currentInsertionStart!!
                )
            )
        }
    }
    return diffs
}

/**
 * Given [base] and [target] we are going to return a [List]<[BlockTextDeletion]> where we indicate the positions we
 * have to delete in [base] so we could get [target].
 *
 * ie:
 *      base: "a big book"
 *      target: "a small book"
 *      result: List<BlockTextDeletion>[(2, 4)] -> so we have to delete from 2 to 4 (included both) in [base]
 *      so we could get [target] from it.
 *
 *      base: "book"
 *      target: "tool"
 *      result: List<BlockTextDeletion>[(0, 0), (3, 3)] -> so we have to delete (0, 0)-> 0 and (3, 3) -> 3 from
 *      [base] so we could get [target] after that (after an insertion).
 *
 * @param base from where we will get the result
 * @param target the difference we will use when comparing against [base]
 * @param changedRanges calculated changed ranges (difference between base and target)
 * @param blockId both [base] and [target] have to belong to the same [Block]
 * @return [List]<[BlockTextDeletion]> positions we have to delete in [base] in order to build [target]
 */
@Suppress("UnsafeCallOnNullableType")
fun textDeletes(base: String, target: String, changedRanges: Pair<Range, Range>, blockId: String):
    List<BlockTextDeletion> {
    val deletes = toDeleteIndices(base, target, changedRanges)
    val diffs = mutableListOf<BlockTextDeletion>()
    var currentDeleteStart: Int? = null
    var lastDeleteIndex: Int? = null

    deletes.forEachIndexed { i, currentDeletionPoint ->
        currentDeleteStart = currentDeleteStart ?: currentDeletionPoint

        val oneLeftOfCurrentDeletionPoint = currentDeletionPoint - 1
        when (lastDeleteIndex) {
            null, oneLeftOfCurrentDeletionPoint -> {
                lastDeleteIndex = currentDeletionPoint
            }
            else -> {
                diffs.add(
                    BlockTextDeletion(
                        blockId, currentDeleteStart!!,
                        lastDeleteIndex!!
                    )
                )
                lastDeleteIndex = currentDeletionPoint
                currentDeleteStart = lastDeleteIndex
            }
        }
        if (i == deletes.size - 1) {
            diffs.add(
                BlockTextDeletion(
                    blockId, currentDeleteStart!!,
                    lastDeleteIndex!!
                )
            )
        }
    }
    return diffs
}

/**
 * Given [baseSpans] and [targetSpans] we are going to return a [List]<[SpanDeletion]> with the
 * [List]<[SpanDeletion]>
 * that we need to delete from [baseSpans] so we can build [targetSpans].
 *
 * ie:
 *      base: listOf(Span(SpanStyle.BOLD, 0, 1, 0))
 *      target: listOf(Span(SpanStyle.BOLD, 0, 1, 0))
 *      result: emptyList() --> we don't have to do anything, [targetSpans] are the same that [baseSpans]
 *
 *      base: listOf(Span(SpanStyle.BOLD_ITALIC, 0, 1, 0), Span(SpanStyle.UNDERLINE, 0, 1, 0)
 *      target: listOf(Span(SpanStyle.UNDERLINE, 0, 1, 0))
 *      result: listOf(SpanDeletion(TEST_BLOCK_ID, Span(SpanStyle.BOLD_ITALIC, 0, 1, 0)))
 *      base had 2 spans, target had just one with underline in the same positions so if we want to have base as
 *      target we have to delete the result Span from the result list.
 *
 * @param baseSpans list of [Span] we are going to modify given the [targetSpans]
 * @param targetSpans list of [Span] we will use to compare against [baseSpans]
 * @param blockId both list of [Span] belong to the same [Block]
 * @return [List<SpanDeletion>] list of Spans we have to delete in order to build [targetSpans] from [baseSpans]
 *
 */
fun spanDeletes(baseSpans: List<Span>, targetSpans: List<Span>, blockId: String): List<SpanDeletion> {
    return baseSpans.filterNot { targetSpans.contains(it) }.map {
        SpanDeletion(blockId, it)
    }
}

/**
 *  Given [baseSpans] and [targetSpans] we are going to return a [List]<[SpanInsertion]> with the needed
 *  [SpanInsertion] that we need to apply in [baseSpans] so we can build [targetSpans]
 *
 *  ie:
 *      base: listOf(Span(SpanStyle.BOLD, 0, 1, 0))
 *      target: listOf(Span(SpanStyle.BOLD, 0, 1, 0))
 *      result: emptyList() --> Nothing to add, [base] has the same [Span] than [target] has.
 *
 *      base: listOf(Span(SpanStyle.BOLD, 0, 1, 0))
 *      target: listOf(Span(SpanStyle.BOLD, 0, 5, 0), Span(SpanStyle.ITALIC, 0, 1, 0))
 *      result: listOf(SpanInsertion("block1",
 *                      Span(SpanStyle.BOLD, 0, 5, 0)),SpanInsertion("block1", Span(SpanStyle.ITALIC, 0, 1, 0)))
 *
 *      base: listOf(Span(SpanStyle.BOLD, 0, 1, 0)),
 *      target: listOf(Span(SpanStyle.BOLD, 3, 5, 0), Span(SpanStyle.ITALIC, 5, 10, 0))
 *      result: listOf(SpanInsertion("block1",
 *      Span(SpanStyle.BOLD, 3, 5, 0)), SpanInsertion("block1", Span(SpanStyle.ITALIC, 5, 10, 0)))
 *
 * @param baseSpans list of [Span] where we will apply the result in order to build [targetSpans]
 * @param targetSpans list of [Span] that we are going to compare with [baseSpans]
 * @param blockId id of the block that the spans belong to.
 * @result list of [SpanInsertion] that we will apply on [baseSpans] in order to build [targetSpans]
 */
fun spanInserts(baseSpans: List<Span>, targetSpans: List<Span>, blockId: String): List<SpanInsertion> {
    return targetSpans.filterNot { baseSpans.contains(it) }.map {
        SpanInsertion(blockId, it)
    }
}

/**
 * Given [base] [Paragraph] and [target] [Paragraph] we are going to obtain a [List]<[BlockDiff]> with the
 * different [BlockDiff] operations we need to apply to [base] in order to build [target].
 *
 * ie:
 *      base: Paragraph(
 *                          localId = "block1", style = ParagraphStyle(),
 *                          content = Content(text = "book", spans = listOf(Span(SpanStyle.BOLD, 1, 2, 0))))
 *      target: Paragraph(
 *                          localId = "block1", style = ParagraphStyle(),
 *                          content = Content(text = "look", spans = listOf(Span(SpanStyle.ITALIC, 1, 2, 0))))
 *      result: listOf(
 *                          BlockTextDeletion("block1", 0, 0), --> we have to delete the range (0, 0) -> 'b'
 *                          BlockTextInsertion("block1", "l", 0), --> we have to insert 'l' in position 0
 *                          SpanInsertion("block1", Span(SpanStyle.ITALIC, 1, 2, 0)), --> new Span
 *                          SpanDeletion("block1", Span(SpanStyle.BOLD, 1, 2, 0))))) --> delete original one since
 *                          one has been added in its same range (1, 2)
 * @param base [Paragraph] where we will have to apply the resulted changes in order to build [target]
 * @param target [Paragraph] whom we will take its content and apply the diff against [base]
 * @result [List]<[BlockDiff]> a list of [BlockDiff] operations that should be applied to [base] in order to build
 * [target].
 */
fun contentDiffs(base: Paragraph, target: Paragraph): List<BlockDiff> {
    val diffs = mutableListOf<BlockDiff>()
    val blockId = base.localId
    val baseText = base.content.text
    val baseSpans = base.content.spans
    val targetText = target.content.text
    val targetSpans = target.content.spans

    val changedRanges = changedRanges(baseText, targetText)
    val textDeletion = textDeletes(baseText, targetText, changedRanges, blockId)
    val textInsertion = textInserts(
        baseText, targetText, changedRanges,
        blockId
    )
    val spansDeletion = spanDeletes(baseSpans, targetSpans, blockId)
    val spansInsertion = spanInserts(baseSpans, targetSpans, blockId)

    diffs.addAll(textDeletion)
    diffs.addAll(textInsertion)
    diffs.addAll(spansDeletion)
    diffs.addAll(spansInsertion)

    return diffs
}

/**
 * Given [base] [Block] and [target] [Block] we are going to return a [List<Diff>] with the [Diff] operations
 * we need to do on [base] so we can build [target] from it.
 * ie:
 *       base: Paragraph(
 *                          localId = "block1", style = ParagraphStyle(),
 *                          content = Content(text = "book", spans = listOf(Span(SpanStyle.BOLD, 1, 2, 0))))
 *      target: Paragraph(
 *                          localId = "block1", style = ParagraphStyle(),
 *                          content = Content(text = "look", spans = listOf(Span(SpanStyle.ITALIC, 1, 2, 0))))
 *      result: listOf(
 *                          BlockTextDeletion("block1", 0, 0), --> we have to delete the range (0, 0) -> 'b'
 *                          BlockTextInsertion("block1", "l", 0), --> we have to insert 'l' in position 0
 *                          SpanInsertion("block1", Span(SpanStyle.ITALIC, 1, 2, 0)), --> new Span
 *                          SpanDeletion("block1", Span(SpanStyle.BOLD, 1, 2, 0))))) --> delete orginal one since
 *                          one has been added in its same range (1, 2)
 *
 *      base: Media(localId = TEST_BLOCK_ID, localUrl= "/media1")
 *      target: Media(localId = TEST_BLOCK_ID, localUrl = "/media2", remoteUrl = "/remote1")
 *      result: listOf(BlockUpdate(Media(localId = TEST_BLOCK_ID, localUrl = "/media2", remoteUrl = "/remote1")))
 *              We are going to update the [base] [Block] with the [target] [Block] completely.
 *
 * @param base [Block] where we should apply the result of this function in order to build [target]
 * @param target we will compare this [Block] with [base] [Block] so we can get the different [Diff] we will
 * have to apply to [base]
 * @result [List]<[Diff]> with the operations we have to apply to [base] in order to build [target]
 */
fun blockDiffs(base: Block, target: Block): List<Diff> {
    val diffs = mutableListOf<Diff>()
    if (base != target) {
        when {
            base is Paragraph && target is Paragraph -> {
                when (Pair(base.isBulleted(), target.isBulleted())) {
                    Pair(true, false) -> diffs.add(
                        UnorderedListDeletion(target.localId)
                    )
                    Pair(false, true) -> diffs.add(
                        UnorderedListInsertion(target.localId)
                    )
                }
                when (Pair(base.isRightToLeft(), target.isRightToLeft())) {
                    Pair(true, false) -> diffs.add(
                        RightToLeftDeletion(target.localId)
                    )
                    Pair(false, true) -> diffs.add(
                        RightToLeftInsertion(target.localId)
                    )
                }
                // content diff
                if (base.content != target.content) {
                    diffs.addAll(contentDiffs(base, target))
                }
            }
            // media updates
            else -> diffs.add(BlockUpdate(target))
        }
    }
    return diffs
}

fun <T> idMap(list: List<T>, id: (item: T) -> String): Map<String, T> {
    val map = mutableMapOf<String, T>()
    list.forEach {
        map.put(id(it), it)
    }
    return map
}

/**
 * It will return a [Map]<[String], [Block]> with tuples of ->  <Block localid, Block itself>.
 * @param blocks the list of [Block] that will be used to map into [Map<String, Block>]
 * @result [Map<String, Block>] being [String] the localId of the [Block] and [Block] the [Block] itself.
 */
fun idToBlockMap(blocks: List<Block>): Map<String, Block> {
    val id = { block: Block -> block.localId }
    return idMap(blocks, id)
}

/**
 * We map the content of this [MutableList]<[Diff]> to [Map]<[String], [MutableList]<[Diff]>>
 * Being the key a [String] that represents [Block] localId and its value a [MutableList]<[Diff]> of all [Diff]
 * operations that [Block] has associated.
 */
internal fun MutableList<Diff>.toMapByBlockId(): Map<String, MutableList<Diff>> {

    val mapped = mutableMapOf<String, MutableList<Diff>>()
    this.forEach { diff ->
        val blockId = when (diff) {
            is BlockDiff -> diff.blockId
            is BlockInsertion -> diff.block.localId
            is BlockUpdate -> diff.block.localId
            else -> null
        }
        blockId?.let {
            val listOfDiffs = mapped[it] ?: mutableListOf()
            listOfDiffs.add(diff)
            mapped[it] = listOfDiffs
        }
    }
    return mapped
}

/**
 * Given [base] [Note] and [target] [Note] we are going to return a [List]<[Diff]> with all [Diff] operations we
 * should do on [base] in order to build [target].
 *
 * ie:
 *      base: Note(color = Color.YELLOW,
 *              document = Document(listOf(Paragraph(content = Content(text = "the body"))))
 *      target: Note(color = Color.BLUE,
 *              document = Document(listOf(Paragraph(content = Content(text = "the body"))))
 *      result: listOf(ColorUpdate(Color.BLUE))
 *
 *      base: Note(document = Document(listOf(
 *                              Paragraph(content = Content(text = "one")),
 *                              Paragraph(content = Content(text = "two")),
 *                              Paragraph(content = Content(text = "three")))))
 *      target: Note(document = Document(listOf(
 *                              Paragraph(content = Content(text = "one")),
 *                              Paragraph(content = Content(text = "two")),
 *                              Paragraph(content = Content(text = "three"),
 *                              Paragraph(content = Content(text = "four")))))
 *      result: listOf(BlockInsertion(block = Paragraph(content = Content(text = "four")), index = 3)
 *
 *
 * @param base the [Note] that will be used in the comparison with [target] so we can have a [List]<[Diff]>
 *     operations that we will apply to it in order to build [target] [Note].
 * @param target the [Note] that will be used to compare against [base] so we can get the different [Diff]
 * operations that we should use in order to build [target] from [base].
 * @result [List]<[Diff]> with the operations we would to apply in [base] in order to build [target].
 */
fun diff(base: Note, target: Note): List<Diff> {
    val diffs = mutableListOf<Diff>()
    val baseBlocks = base.document.blocks
    val targetBlocks = target.document.blocks
    val baseMediaList = base.media
    val targetMediaList = target.media
    val baseIdToBlockMap = idToBlockMap(baseBlocks)
    val targetIdToBlockMap = idToBlockMap(targetBlocks)
    val baseIdToMediaMap = idToMediaMap(baseMediaList)
    val targetIdToMediaMap = idToMediaMap(targetMediaList)

    if (base.color != target.color) diffs.add(ColorUpdate(target.color))

    // blocks
    baseIdToBlockMap.keys.forEach {
        if (!targetIdToBlockMap.containsKey(it)) diffs.add(BlockDeletion(it))
    }

    targetBlocks.forEachIndexed { index, targetBlock ->
        val baseBlock = baseIdToBlockMap[targetBlock.localId]
        when (baseBlock) {
            null -> diffs.add(BlockInsertion(targetBlock, index))
            else -> diffs.addAll(blockDiffs(baseBlock, targetBlock))
        }
    }

    // media
    baseIdToMediaMap.keys.forEach {
        if (!targetIdToMediaMap.containsKey(it)) diffs.add(MediaDeletion(it))
    }

    targetMediaList.forEachIndexed { index, targetMedia ->
        val baseMedia = baseIdToMediaMap[targetMedia.localId]
        when (baseMedia) {
            null -> diffs.add(
                MediaInsertion(targetMedia.localId, targetMedia, index)
            )
            else -> diffs.addAll(mediaDiffs(baseMedia, targetMedia))
        }
    }

    return diffs
}
