package com.microsoft.notes.richtext.editor.styled

import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import java.util.regex.Pattern

private const val IMG_SRC_CID_PREFIX = "cid:"
private val MIN_HTML_SUPPORT_VERSION = Version("2.0.1")

class Version(versionString: String) {
    private val tokens: List<Int> = when (versionString.isEmpty()) {
        true -> emptyList()
        false -> versionString.split('.').map { it.toInt() }
    }

    fun compare(v2: Version): Int {
        val it1: ListIterator<Int> = tokens.listIterator()
        val it2: ListIterator<Int> = v2.tokens.listIterator()

        while (it1.hasNext() && it2.hasNext()) {
            val e1 = it1.next()
            val e2 = it2.next()

            if (e1 > e2) return 1
            else if (e1 < e2) return -1
        }
        return when {
            it1.hasNext() -> 1
            it2.hasNext() -> -1
            else -> 0
        }
    }
}

fun shouldRenderAsHTML(note: Note): Boolean {
    if (!NotesLibrary.getInstance().experimentFeatureFlags.samsungNoteHtmlRenderingEnabled)
        return false

    return Version(note.document.dataVersion).compare(MIN_HTML_SUPPORT_VERSION) >= 0
}

fun prepareNoteHTML(note: Note): String {
    val cidToLocalIdMap = note.media.associate { it.localId to it.localUrl }
    val mapper: (String) -> String = { cid ->
        cidToLocalIdMap.get(cid)
            ?: "file:///android_res/drawable/sn_notes_canvas_image_placeholder.png"
    }

    val noteHtml = replaceImageSrcWithLocalUrl(html = note.document.body, cidToLocalSrcMapper = mapper)
    // wrapping in separate div to fix image scaling issues
    // also, setting html body margin to 0
    val wrappedHtml = "<body style=\"margin: 0; padding: 0\"> <div style=\"display:flex;flex-direction:column\">$noteHtml</div>"
    return wrappedHtml
}

// Replace inlined html images src referenced by 'cid' with the actual local image file url
private fun replaceImageSrcWithLocalUrl(html: String, cidToLocalSrcMapper: (String) -> String?): String {
    val newHtml = StringBuilder()
    var lastReadIndex = -1

    val imageSrcRanges = getImageSrcRangesSortedFromHtml(html)
    for (srcRange in imageSrcRanges) {
        val src = html.substring(srcRange.first, srcRange.second)
        if (src.startsWith(IMG_SRC_CID_PREFIX)) {
            val cid = src.substring(IMG_SRC_CID_PREFIX.length, src.length)
            val localId = cidToLocalSrcMapper(cid)
            if (localId != null) {
                newHtml.append(html.substring(lastReadIndex + 1, srcRange.first) + localId)
                lastReadIndex = srcRange.second - 1
            }
        }
    }
    newHtml.append(html.substring(lastReadIndex + 1, html.length))
    return newHtml.toString()
}

private const val HTML_IMG_SRC_ELEMENT_REGEX = "<img [^>]*src[\\s]*=[\\s]*\""
private val imgSrcHTMLElementPattern: Pattern = Pattern.compile(
    HTML_IMG_SRC_ELEMENT_REGEX,
    Pattern.DOTALL or Pattern.MULTILINE
)

/**
 * Returns a list of disjoint imageSrc-Ranges in the ordered by their appearance in html string
 * imageSrc-Range: Pair<startIndex inclusive, EndIndex exclusive>
 *     range describes the 'src' attribute of element 'img' in html
 */
@Suppress("LoopWithTooManyJumpStatements")
fun getImageSrcRangesSortedFromHtml(html: String): List<Pair<Int, Int>> {
    val matcher = imgSrcHTMLElementPattern.matcher(html)
    val imageSrcList = mutableListOf<Pair<Int, Int>>()
    while (matcher.find()) {
        val srcStart = matcher.end()
        if (srcStart == -1)
            continue
        val srcEnd = html.indexOf('"', srcStart)
        if (srcEnd == -1)
            continue

        imageSrcList.add(Pair(srcStart, srcEnd))
    }
    return imageSrcList
}
