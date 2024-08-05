package com.microsoft.notes.sync.models

import com.microsoft.notes.sync.JSON
import com.microsoft.notes.sync.sequence
import java.io.Serializable

sealed class Document : Serializable {
    enum class DocumentType {
        RICH_TEXT,
        HTML,

        /**
         * Rendered ink is passed from the API as a PNG
         */
        RENDERED_INK,

        /**
         * Ink is passed from the API as a strokes array
         */
        INK,
        FUTURE
    }

    companion object {
        const val RENDERED_INK_DOCUMENT_ID = "renderedInk"
        const val INK_DOCUMENT_ID = "ink"
        const val RICH_TEXT_DOCUMENT_ID = "document"
        const val HTML_DOCUMENT_ID = "html"
        const val FUTURE_DOCUMENT_ID = "future"

        fun fromJSON(json: JSON.JObject): Document? {
            val type = json.get<JSON.JString>("type")?.string ?: return null
            return when (type) {
                RENDERED_INK_DOCUMENT_ID -> RenderedInkDocument.fromJSON(json)
                INK_DOCUMENT_ID -> InkDocument.fromJSON(json)
                RICH_TEXT_DOCUMENT_ID -> RichTextDocument.fromJSON(json)
                HTML_DOCUMENT_ID -> SamsungHtmlDocument.fromJSON(json)
                FUTURE_DOCUMENT_ID -> FutureDocument.fromJSON(json)
                else -> null
            }
        }

        fun fromMap(map: Map<String, Any>): Document? {
            val type = map["type"] as? String ?: return null
            return when (type) {
                DocumentType.RENDERED_INK.name -> RenderedInkDocument.fromMap(map)
                DocumentType.INK.name -> InkDocument.fromMap(map)
                DocumentType.RICH_TEXT.name -> RichTextDocument.fromMap(map)
                DocumentType.HTML.name -> SamsungHtmlDocument.fromMap(map)
                else -> null
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old <= 0 || old >= new) return json

            val map = json as? Map<String, Any> ?: return json

            val type = map["type"] as? String ?: return map
            return when (type) {
                DocumentType.RENDERED_INK.name -> RenderedInkDocument.migrate(map, old, new)
                DocumentType.INK.name -> InkDocument.migrate(map, old, new)
                DocumentType.RICH_TEXT.name -> RichTextDocument.migrate(map, old, new)
                DocumentType.HTML.name -> SamsungHtmlDocument.migrate(map, old, new)
                else -> map
            }
        }
    }

    abstract fun toJSON(): JSON.JObject

    abstract val type: DocumentType

    data class RenderedInkDocument(val text: String, val image: String) : Document() {
        val mimeType = "image/png"

        companion object {
            fun fromJSON(json: JSON.JObject): RenderedInkDocument? {
                val text = json.get<JSON.JString>("text")?.string ?: return null
                val image = json.get<JSON.JString>("image")?.string ?: return null
                return RenderedInkDocument(text, image)
            }

            fun fromMap(map: Map<String, Any>): RenderedInkDocument? {
                val text = map.get("text") as? String ?: return null
                val image = map.get("image") as? String ?: return null
                return RenderedInkDocument(text, image)
            }

            fun migrate(json: Any, old: Int, new: Int): Any {
                if (old <= 0 || old >= new) return json

                return json
            }
        }

        override fun toJSON(): JSON.JObject {
            return JSON.JObject(
                hashMapOf(
                    "text" to JSON.JString(this.text),
                    "image" to JSON.JString(this.image),
                    "type" to JSON.JString(RENDERED_INK_DOCUMENT_ID)
                )
            )
        }

        override val type = DocumentType.RENDERED_INK
    }

    data class InkDocument(val text: String, val strokes: List<Stroke>) : Document() {
        companion object {
            fun fromJSON(json: JSON.JObject): InkDocument? {
                val text = json.get<JSON.JString>("text")?.string ?: return null
                val strokes = json.get<JSON.JArray>("strokes")?.array?.map {
                    (it as? JSON.JObject)?.let { Stroke.fromJSON(it) }
                }?.sequence() ?: return null
                return InkDocument(text, strokes)
            }

            @Suppress("UNCHECKED_CAST")
            fun fromMap(map: Map<String, Any>): InkDocument? {
                val text = map.get("text") as? String ?: return null
                val strokes = (map["strokes"] as? List<Map<String, Any>> ?: return null)
                    .mapNotNull { stroke: Map<String, Any> -> Stroke.fromMap(stroke) }
                return InkDocument(text, strokes)
            }

            // Currently not applicable
            fun migrate(json: Any, old: Int, new: Int): Any = json
        }

        override fun toJSON(): JSON.JObject {
            return JSON.JObject(
                hashMapOf(
                    "text" to JSON.JString(this.text),
                    "strokes" to JSON.JArray(this.strokes.map { it.toJSON() }),
                    "type" to JSON.JString(INK_DOCUMENT_ID)
                )
            )
        }

        override val type = DocumentType.INK
    }

    data class RichTextDocument(val blocks: List<Block>) : Document() {
        companion object {
            fun fromJSON(json: JSON.JObject): RichTextDocument? {
                val blocks = json.get<JSON.JArray>("blocks")?.toList()?.map {
                    Block.fromJSON(it)
                }?.sequence() ?: return null
                return RichTextDocument(blocks)
            }

            @Suppress("UNCHECKED_CAST")
            fun fromMap(map: Map<String, Any>): RichTextDocument? {
                if (map.containsKey("blocks")) {
                    val blocks = (map.get("blocks") as? List<Map<String, Any>> ?: return null)
                        .mapNotNull { block: Map<String, Any> -> Block.fromMap(block) }
                    return RichTextDocument(blocks)
                } else {
                    return null
                }
            }

            @Suppress("UNCHECKED_CAST", "UnsafeCast")
            fun migrate(json: Any, old: Int, new: Int): Any {
                if (old <= 0 || old >= new) return json

                val map = (json as? Map<String, Any> ?: return json).toMutableMap()

                if (map.get("blocks") as? List<Map<String, Any>> != null) {
                    val migrated_blocks = (map.get("blocks") as List<Map<String, Any>>)
                        .map { block: Map<String, Any> -> Block.migrate(block, old, new) }
                    map["blocks"] = migrated_blocks
                }

                return map
            }
        }

        override fun toJSON(): JSON.JObject {
            return JSON.JObject(
                hashMapOf(
                    "blocks" to JSON.JArray(this.blocks.map { it.toJSON() }),
                    "type" to JSON.JString(RICH_TEXT_DOCUMENT_ID)
                )
            )
        }

        override val type = DocumentType.RICH_TEXT
    }

    data class SamsungHtmlDocument(val body: String, val bodyPreview: String, val blocks: List<Block>, val dataVersion: String) : Document() {
        companion object {
            fun fromJSON(json: JSON.JObject): SamsungHtmlDocument? {
                val body = json.get<JSON.JString>("body")?.string ?: return null
                val bodyPreview = json.get<JSON.JString>("bodyPreview")?.string ?: ""
                val blocks = json.get<JSON.JArray>("blocks")?.toList()?.map {
                    Block.fromJSON(it)
                }?.sequence() ?: return null
                val dataVersion = json.get<JSON.JString>("dataVersion")?.string ?: DEFAULT_DATA_VERSION
                return SamsungHtmlDocument(body, bodyPreview, blocks, dataVersion)
            }

            @Suppress("UNCHECKED_CAST")
            fun fromMap(map: Map<String, Any>): SamsungHtmlDocument? {
                if ("body" in map && "bodyPreview" in map && "blocks" in map) {
                    val body = map["body"] as? String ?: return null
                    val bodyPreview = map["bodyPreview"] as? String ?: ""
                    val blocks = (map.get("blocks") as? List<Map<String, Any>> ?: return null)
                        .mapNotNull { block: Map<String, Any> -> Block.fromMap(block) }
                    val dataVersion = map["dataVersion"] as? String ?: DEFAULT_DATA_VERSION
                    return SamsungHtmlDocument(body, bodyPreview, blocks, dataVersion)
                } else {
                    return null
                }
            }

            @Suppress("UNCHECKED_CAST", "UnsafeCast")
            fun migrate(json: Any, old: Int, new: Int): Any = json

            const val DEFAULT_DATA_VERSION = "1.0.0"
        }

        override fun toJSON(): JSON.JObject {
            return JSON.JObject(
                hashMapOf(
                    "body" to JSON.JString(this.body),
                    "bodyPreview" to JSON.JString(this.bodyPreview),
                    "blocks" to JSON.JArray(this.blocks.map { it.toJSON() }),
                    "type" to JSON.JString(HTML_DOCUMENT_ID),
                    "dataVersion" to JSON.JString(this.dataVersion)
                )
            )
        }

        override val type = DocumentType.HTML
    }

    /*
     * Required to add a dummy data member in primary constructor as it is data class, this is not for use
     */
    data class FutureDocument(val dummyDataMember: String = "") : Document() {
        companion object {
            fun fromJSON(@Suppress("UNUSED_PARAMETER") json: JSON.JObject): FutureDocument? = FutureDocument()
        }

        override fun toJSON(): JSON.JObject = JSON.JObject(hashMapOf("type" to JSON.JString(FUTURE_DOCUMENT_ID)))

        override val type = DocumentType.FUTURE
    }
}

data class Stroke(val id: String, val points: List<InkPoint>) : Serializable {
    companion object {
        fun fromJSON(json: JSON.JObject): Stroke? {
            val id = json.get<JSON.JString>("id")?.string ?: return null
            val points = json.get<JSON.JArray>("points")?.array?.map {
                (it as? JSON.JObject)?.let { InkPoint.fromJSON(it) }
            }?.sequence() ?: return null
            return Stroke(id = id, points = points)
        }

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any>): Stroke? {
            val id = map["id"] as? String ?: return null
            val points = (map["points"] as? List<Map<String, Any>> ?: return null)
                .mapNotNull { inkPoint: Map<String, Any> -> InkPoint.fromMap(inkPoint) }
            return Stroke(id = id, points = points)
        }

        // Currently not applicable
        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any = json
    }

    fun toJSON(): JSON.JObject {
        return JSON.JObject(
            hashMapOf(
                "id" to JSON.JString(this.id),
                "points" to JSON.JArray(this.points.map { it.toJSON() })
            )
        )
    }
}

data class InkPoint(val x: Double, val y: Double, val p: Double) : Serializable {
    companion object {
        fun fromJSON(json: JSON.JObject): InkPoint? {
            val x = json.get<JSON.JNumber>("x")?.number ?: return null
            val y = json.get<JSON.JNumber>("y")?.number ?: return null
            val p = json.get<JSON.JNumber>("p")?.number ?: return null
            return InkPoint(x = x, y = y, p = p)
        }

        fun fromMap(map: Map<String, Any>): InkPoint? {
            val x = map["x"] as? Double ?: return null
            val y = map["y"] as? Double ?: return null
            val p = map["p"] as? Double ?: return null
            return InkPoint(x = x, y = y, p = p)
        }

        // Currently not applicable
        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any = json
    }

    fun toJSON(): JSON.JObject {
        return JSON.JObject(
            hashMapOf(
                "x" to JSON.JNumber(this.x.toDouble()),
                "y" to JSON.JNumber(this.y.toDouble()),
                "p" to JSON.JNumber(this.p.toDouble())
            )
        )
    }
}

sealed class Block : Serializable {
    enum class BlockType {
        Paragraph,
        InlineMedia
    }

    companion object {
        fun fromJSON(json: JSON): Block? {
            val obj = json as? JSON.JObject ?: return null
            val type = obj.get<JSON.JString>("type")?.string
            return when (type) {
                "paragraph" -> Paragraph.fromJSON(obj)
                "media" -> InlineMedia.fromJSON(obj)
                else -> null
            }
        }

        fun fromMap(map: Map<String, Any>): Block? {
            val type = map.get("type") as? String ?: return null
            return when (type) {
                BlockType.Paragraph.name -> Paragraph.fromMap(map)
                BlockType.InlineMedia.name -> InlineMedia.fromMap(map)
                else -> null
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old <= 0 || old >= new) return json

            val map = json as? Map<String, Any> ?: return json

            val type = map.get("type") as? String ?: return map
            return when (type) {
                BlockType.Paragraph.name -> Paragraph.migrate(map, old, new)
                BlockType.InlineMedia.name -> InlineMedia.migrate(map, old, new)
                else -> map
            }
        }
    }

    abstract fun toJSON(): JSON.JObject

    abstract val type: BlockType

    data class Paragraph(
        val id: String,
        val content: List<ParagraphChunk>,
        val blockStyles: BlockStyle
    ) : Block(), Serializable {
        companion object {
            fun fromJSON(json: JSON.JObject): Paragraph? {
                val id = json.get<JSON.JString>("id")?.string ?: return null
                val content = json.get<JSON.JArray>("content")?.array?.map {
                    ParagraphChunk.fromJSON(it)
                }?.sequence() ?: return null
                val blockStylesJson = json.get<JSON.JObject>("blockStyles")
                val blockStyles = if (blockStylesJson != null) {
                    BlockStyle.fromJSON(blockStylesJson)
                } else {
                    BlockStyle()
                }

                return Paragraph(id = id, content = content, blockStyles = blockStyles)
            }

            @Suppress("UNCHECKED_CAST")
            fun fromMap(map: Map<String, Any>): Paragraph? {
                val id = map.get("id") as? String ?: return null
                val content = (map.get("content") as? List<Map<String, Any>> ?: return null)
                    .mapNotNull { paragraphChunk: Map<String, Any> -> ParagraphChunk.fromMap(paragraphChunk) }
                val blockStyles = BlockStyle.fromMap(map.get("blockStyles") as? Map<String, Any> ?: return null)

                return Paragraph(id = id, content = content, blockStyles = blockStyles)
            }

            fun migrate(json: Any, old: Int, new: Int): Any {
                if (old <= 0 || old >= new) return json

                return json
            }
        }

        override val type = BlockType.Paragraph

        override fun toJSON(): JSON.JObject {
            return JSON.JObject(
                hashMapOf(
                    "id" to JSON.JString(this.id),
                    "type" to JSON.JString("paragraph"),
                    "content" to JSON.JArray(this.content.map { it.toJSON() }),
                    "blockStyles" to blockStyles.toJSON()
                )
            )
        }
    }

    data class InlineMedia(
        val id: String,
        val source: String?,
        val mimeType: String,
        val altText: String?
    ) : Block(), Serializable {
        companion object {
            fun fromJSON(json: JSON.JObject): InlineMedia? {
                val id = json.get<JSON.JString>("id")?.string ?: return null
                val source = json.get<JSON.JString>("source")?.string
                val mimeType = json.get<JSON.JString>("mimeType")?.string ?: return null
                val altText = json.get<JSON.JString>("altText")?.string
                return InlineMedia(id = id, source = source, mimeType = mimeType, altText = altText)
            }

            fun fromMap(map: Map<String, Any>): InlineMedia? {
                val id = map.get("id") as? String ?: return null
                val source = map.get("source") as? String
                val mimeType = map.get("mimeType") as? String ?: return null
                val altText = map.get("altText") as? String
                return InlineMedia(id = id, source = source, mimeType = mimeType, altText = altText)
            }

            @Suppress("UNCHECKED_CAST")
            fun migrate(json: Any, old: Int, new: Int): Any {
                if (old <= 0 || old >= new) return json

                return json
            }
        }

        override val type = BlockType.InlineMedia

        override fun toJSON(): JSON.JObject {
            return JSON.JObject(
                hashMapOf(
                    "id" to JSON.JString(this.id),
                    "type" to JSON.JString("media"),
                    "source" to (this.source?.let { JSON.JString(it) } ?: JSON.JNull()),
                    "mimeType" to JSON.JString(this.mimeType),
                    "altText" to (this.altText?.let { JSON.JString(it) } ?: JSON.JNull())
                )
            )
        }
    }
}

data class BlockStyle(val unorderedList: Boolean = false, val rightToLeft: Boolean = false) : Serializable {

    companion object {
        const val BULLET_STYLE_ID = "listType"
        const val BULLET_STYLE_VALUE = "bullet"
        const val TEXT_DIRECTION_ID = "textDirection"
        const val TEXT_DIRECTION_VALUE = "rtl"

        fun fromJSON(json: JSON): BlockStyle {
            val obj = json as? JSON.JObject ?: return BlockStyle()
            val unorderedList: Boolean = obj.get<JSON.JString>(BULLET_STYLE_ID)?.string?.equals(
                BULLET_STYLE_VALUE
            ) ?: false
            val rightToLeft: Boolean = obj.get<JSON.JString>(TEXT_DIRECTION_ID)?.string?.equals(
                TEXT_DIRECTION_VALUE
            ) ?: false
            return BlockStyle(unorderedList, rightToLeft)
        }

        fun fromMap(map: Map<String, Any>): BlockStyle {
            val unorderedList = map["unorderedList"] as? Boolean ?: false
            val rightToLeft = map["rightToLeft"] as? Boolean ?: false
            return BlockStyle(unorderedList, rightToLeft)
        }
    }

    fun toJSON(): JSON.JObject {
        val map = mutableMapOf<String, JSON.JString>()
        if (unorderedList) map[BULLET_STYLE_ID] = JSON.JString(BULLET_STYLE_VALUE)
        if (rightToLeft) map[TEXT_DIRECTION_ID] = JSON.JString(TEXT_DIRECTION_VALUE)
        return JSON.JObject(map)
    }
}

sealed class ParagraphChunk : Serializable {
    enum class ParagraphChunkType {
        PlainText,
        RichText
    }

    companion object {
        fun fromJSON(json: JSON): ParagraphChunk? {
            return when (json) {
                is JSON.JString -> {
                    PlainText(json.string)
                }
                is JSON.JObject -> {
                    val text = json.get<JSON.JString>("text")?.string ?: return null
                    val styles = json.get<JSON.JArray>("styles")?.array?.map {
                        (it as? JSON.JString)?.let { InlineStyle.fromJSON(it) }
                    }?.sequence() ?: return null
                    RichText(text, styles)
                }
                else -> {
                    null
                }
            }
        }

        fun fromMap(map: Map<String, Any>): ParagraphChunk? {
            val type = map.get("type") as? String ?: return null
            return when (type) {
                ParagraphChunkType.PlainText.name -> PlainText.fromMap(map)
                ParagraphChunkType.RichText.name -> RichText.fromMap(map)
                else -> null
            }
        }
    }

    abstract val type: ParagraphChunkType

    abstract fun toJSON(): JSON

    data class PlainText(val string: String) : ParagraphChunk(), Serializable {
        override val type = ParagraphChunkType.PlainText

        override fun toJSON(): JSON = JSON.JString(this.string)

        companion object {
            fun fromMap(map: Map<String, Any>): PlainText? {
                val string = map.get("string") as? String ?: return null
                return PlainText(string)
            }
        }
    }

    data class RichText(
        val string: String,
        val inlineStyles: List<InlineStyle>
    ) : ParagraphChunk(), Serializable {
        override val type = ParagraphChunkType.RichText
        override fun toJSON(): JSON {
            return JSON.JObject(
                hashMapOf(
                    "text" to JSON.JString(this.string),
                    "styles" to JSON.JArray(this.inlineStyles.map { it.toJSON() })
                )
            )
        }

        companion object {
            @Suppress("UNCHECKED_CAST")
            fun fromMap(map: Map<String, Any>): RichText? {
                val string = map.get("string") as? String ?: return null

                val inlineStyles = (map.get("inlineStyles") as? List<String> ?: return null)
                    .map { inlineStyle: String ->
                        InlineStyle.valueOf(inlineStyle) /* throws exception, should we catch? */
                    }
                return RichText(string, inlineStyles)
            }
        }
    }
}

enum class InlineStyle : Serializable {
    Bold,
    Italic,
    Underlined,
    Strikethrough;

    companion object {
        fun fromJSON(json: JSON.JString): InlineStyle? {
            return when (json.string) {
                "bold" -> InlineStyle.Bold
                "italic" -> InlineStyle.Italic
                "underlined" -> InlineStyle.Underlined
                "strikethrough" -> InlineStyle.Strikethrough
                else -> null
            }
        }
    }

    fun toJSON(): JSON.JString {
        return when (this) {
            Bold -> JSON.JString("bold")
            Italic -> JSON.JString("italic")
            Underlined -> JSON.JString("underlined")
            Strikethrough -> JSON.JString("strikethrough")
        }
    }
}
