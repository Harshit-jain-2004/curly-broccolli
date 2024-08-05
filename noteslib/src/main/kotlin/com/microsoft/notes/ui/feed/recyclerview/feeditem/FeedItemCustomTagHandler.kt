package com.microsoft.notes.ui.feed.recyclerview.feeditem

import android.content.Context
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.text.style.ImageSpan
import android.text.style.LeadingMarginSpan
import androidx.core.content.ContextCompat
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.render.NotesBulletSpan
import com.microsoft.notes.richtext.render.TextLeadingMarginSpan
import org.xml.sax.XMLReader
import java.lang.reflect.Field
import java.util.Stack
import kotlin.collections.HashMap

class FeedItemCustomTagHandler(val context: Context, private val drawableBounds: Int) : Html.TagHandler {
    companion object {
        private const val LOG_TAG = "FeedItemListTagHandler"
        const val UL_TAG = "unordered"
        const val ICON_TAG = "icon"
        const val OL_TAG = "ordered"
        const val LI_TAG = "listitem"

        /**
         * Ordered lists and nested unordered lists are not supported by Html.fromHtml
         * We are modifying ul, ol and li tags to custom tags, so that we can handle
         * them in FeedItemListTagHandler
         */
        fun prepareHtmlForListTagHandler(html: String): String {
            return html
                .replace("\\r\\n", "\n")
                .replace("(?i)<ul([^>]*)>".toRegex(), "<$UL_TAG$1>")
                .replace("(?i)</ul>".toRegex(), "</$UL_TAG>")
                .replace("(?i)<ol([^>]*)>".toRegex(), "<$OL_TAG$1>")
                .replace("(?i)</ol>".toRegex(), "</$OL_TAG>")
                .replace("(?i)<li([^>]*)>".toRegex(), "<$LI_TAG$1>")
                .replace("(?i)</li>".toRegex(), "</$LI_TAG>")
                .replace("<i\\s+class=(\\S+)\\s*/>".toRegex(), "<$ICON_TAG class=$1 />")
                // We need to take special care while handling HTML received from local signals,
                // as it may not include self-closing icon tags and may also include an extra non-breaking space character.
                .replace("<i\\s+class=(\\S+)>(&nbsp;)?</i>&nbsp;".toRegex(), "<$ICON_TAG class=$1 /> ")
        }
    }

    /**
     * This enum contains the Note Tag types
     * that are currently supported
     */
    enum class NoteTagIcon(private val noteTagHashId: String) {
        UncheckedTodo("3|0"),
        CheckedTodo("3|1");

        fun getNoteTagHashId(): String = noteTagHashId

        companion object {
            fun getDrawable(noteTagHashId: String): Int? {
                return when (noteTagHashId) {
                    CheckedTodo.getNoteTagHashId() -> R.drawable.ic_checked_todo
                    UncheckedTodo.getNoteTagHashId() -> R.drawable.ic_unchecked_todo
                    else -> null
                }
            }
        }
    }

    /**
     * This enum contains the Ordered List Tag types
     * that are currently supported
     */
    private enum class OrderedListTagType(val value: Char) {
        NUMERIC('1'),
        ALPHABETICAL_LOWERCASE('a'),
        ALPHABETICAL_UPPERCASE('A');

        companion object {
            private val OrderedListTagTypeMap = values().associateBy(
                OrderedListTagType::value
            )
            fun fromChar(orderedListTag: Char): OrderedListTagType =
                OrderedListTagTypeMap[orderedListTag] ?: NUMERIC
        }
    }

    private val listStack: Stack<ListTag> = Stack()
    private val attributes: HashMap<String, String> = HashMap()
    private var isValidScenarioToHandleIconClosingTag = false

    private fun handleIconOpeningTag(output: Editable, xmlReader: XMLReader?) {
        processAttributes(xmlReader)
        try {
            val classValue = attributes.getValue("class")
            val drawableId: Int = NoteTagIcon.getDrawable(classValue) ?: return
            val drawable = ContextCompat.getDrawable(context, drawableId)
            drawable?.setBounds(0, 0, drawableBounds, drawableBounds)
            val imageSpan = drawable?.let { ImageSpan(it) }
            output.append(" ")
            output.setSpan(
                imageSpan,
                output.length - 1,
                output.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            isValidScenarioToHandleIconClosingTag = true
        } catch (e: Exception) {
            NotesLibrary.getInstance().log("Exception: $e")
        }
    }

    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader?) {
        val indentation = listStack.size

        if (tag == ICON_TAG) {
            if (opening) {
                handleIconOpeningTag(output, xmlReader)
            } else {
                if (isValidScenarioToHandleIconClosingTag) {
                    output.append("  ")
                    output.setSpan(TextLeadingMarginSpan("", R.integer.feed_preview_card_checklist_leading_text_margin), output.length, output.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                    isValidScenarioToHandleIconClosingTag = false
                }
            }
        } else if (tag == UL_TAG) {
            if (opening) {
                listStack.push(UnorderedListTag(indentation))
            } else {
                listStack.pop()
            }
        } else if (tag == OL_TAG) {
            if (opening) {
                listStack.push(OrderedListTag(indentation, getListType(xmlReader, indentation)))
            } else {
                listStack.pop()
            }
        } else if (tag == LI_TAG) {
            if (listStack.isNotEmpty()) {
                if (opening) {
                    listStack.peek().handleItemOpen(output)
                } else {
                    listStack.peek().handleItemClose(output)
                }
            }
        }
    }

    /**
     * Returns the type property for the Ordered list tag.
     * Cycles through the supported types values in OrderedListTagType
     * as a fallback, if type couldn't be retrieved or is unsupported
     */
    private fun getListType(xmlReader: XMLReader?, indentation: Int): OrderedListTagType {
        processAttributes(xmlReader)

        val typeAttr = attributes["type"]
        val orderedListTagTypeVals = OrderedListTagType.values()
        return if (typeAttr.isNullOrEmpty() || typeAttr.length > 1 || !orderedListTagTypeVals.any { it.value == typeAttr[0] })
            orderedListTagTypeVals[indentation % orderedListTagTypeVals.size]
        else
            OrderedListTagType.fromChar(typeAttr[0])
    }

    /**
     * Attributes aren't passed in the handleTag method in the Android source code.
     * So we try to get the attributes using reflection
     */
    private fun processAttributes(xmlReader: XMLReader?) {
        attributes.clear()
        if (xmlReader != null) {
            try {
                val elementField: Field = xmlReader.javaClass.getDeclaredField("theNewElement")
                elementField.isAccessible = true
                val element: Any = elementField.get(xmlReader)
                val attsField: Field = element.javaClass.getDeclaredField("theAtts")
                attsField.isAccessible = true
                val atts: Any = attsField.get(element)
                val dataField: Field = atts.javaClass.getDeclaredField("data")
                dataField.isAccessible = true
                val data = dataField.get(atts) as Array<String>
                val lengthField: Field = atts.javaClass.getDeclaredField("length")
                lengthField.isAccessible = true
                val len = lengthField.get(atts) as Int

                for (i in 0 until len) {
                    attributes[data[i * 5 + 1]] = data[i * 5 + 4]
                }
            } catch (e: Exception) {
                NotesLibrary.getInstance().log("Exception: $e")
            }
        }
    }

    /**
     * This class acts as a mark which helps us get the
     * starting point of the list item when we handle
     * list item's closing tag
     */
    private class ListItemMark

    private interface ListTag {
        fun handleItemOpen(output: Editable) {
            try {
                output.setSpan(ListItemMark(), output.length, output.length, Spannable.SPAN_MARK_MARK)
            } catch (e: Exception) {
                NotesLibrary.getInstance().log("Failed to set ListMark Span: $e")
            }
        }

        fun handleItemClose(output: Editable)

        fun setListItemSpan(output: Editable, span: LeadingMarginSpan, start: Int, end: Int, flags: Int) {
            try {
                output.setSpan(span, start, end, flags)
            } catch (e: Exception) {
                NotesLibrary.getInstance().log("Failed to set Paragraph Span: $e")
            }
        }
    }

    private class UnorderedListTag(val indentation: Int) : ListTag {

        override fun handleItemClose(output: Editable) {
            output.append("\n")
            val lastMark = output.getSpans(0, output.length, ListItemMark().javaClass).lastOrNull()
            lastMark?.let {
                val start = output.getSpanStart(it)
                output.removeSpan(it)
                if (start != output.length) {
                    setListItemSpan(output, NotesBulletSpan(indentation), start, output.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    private class OrderedListTag(val indentation: Int, val listType: OrderedListTagType) : ListTag {
        private var index = 0

        override fun handleItemClose(output: Editable) {
            output.append("\n")
            val lastMark = output.getSpans(0, output.length, ListItemMark().javaClass).lastOrNull()
            lastMark?.let {
                val listIndexStr = getMarginText()
                val start = output.getSpanStart(it)
                output.removeSpan(it)
                if (start != output.length) {
                    setListItemSpan(output, TextLeadingMarginSpan(listIndexStr, indentation), start, output.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            }

            index++
        }

        /**
         * This returns the text we are going to show in the Leading margin,
         * for numeric type, we convert the index to string after incrementing it
         * since index is 0 indexed
         *
         * For Alphabetical types we are just increasing the char value by index
         * This will break if we show more than 26 lines in preview
         */
        fun getMarginText(): String {
            return if (listType == OrderedListTagType.NUMERIC) {
                (index + 1).toString()
            } else {
                listType.value.plus(index).toString()
            }
        }
    }
}
