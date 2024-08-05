package com.microsoft.notes.richtext.scheme

import java.io.Serializable
import java.util.UUID

const val LOCAL_ID = "localId"
fun generateLocalId(): String = LOCAL_ID + "_" + UUID.randomUUID().toString().replace("-".toRegex(), "")

enum class BlockType { Paragraph, InlineMedia }

sealed class Block : Serializable {
    abstract val localId: String
    abstract val blockType: BlockType

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old <= 0 || old >= new) return json

            val map = json as? Map<String, Any> ?: return json

            val type = map.get("blockType") as? String ?: return json
            if (type == BlockType.InlineMedia.name) {
                return InlineMedia.migrate(map, old, new)
            } else if (type == BlockType.Paragraph.name) {
                return Paragraph.migrate(map, old, new)
            }

            return map
        }
    }
}

data class Paragraph(
    override val localId: String = generateLocalId(),
    val style: ParagraphStyle = ParagraphStyle(),
    val content: Content = Content()
) : Block() {
    override val blockType: BlockType = BlockType.Paragraph

    fun isBulleted(): Boolean = this.style.unorderedList

    fun isRightToLeft(): Boolean = this.style.rightToLeft

    companion object {
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old <= 0 || old >= new) return json

            return json
        }
    }
}

data class InlineMedia(
    override val localId: String = generateLocalId(),
    val localUrl: String? = null,
    val remoteUrl: String? = null,
    val mimeType: String = "",
    val altText: String? = null
) : Block() {
    override val blockType: BlockType = BlockType.InlineMedia

    companion object {
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old <= 0 || old >= new) return json

            return json
        }
    }
}

data class ParagraphStyle(val unorderedList: Boolean = false, val rightToLeft: Boolean = false) : Serializable
