package com.microsoft.notes.ui.feed.recyclerview

enum class FeedLayoutType(val value: Int) {
    LIST_LAYOUT(0),
    GRID_LAYOUT(1);

    companion object {
        private val layoutMap = values().associateBy(
            FeedLayoutType::value
        )
        fun fromInt(layoutType: Int): FeedLayoutType = layoutMap[layoutType] ?: LIST_LAYOUT
    }
}
