package com.microsoft.notes.ui.feed.sourcefilter

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Checkable
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.theme.ThemedFrameLayout
import kotlinx.android.synthetic.main.sn_feed_source_filter_option.view.*

class FeedSourceFilterOptionCheckbox(context: Context, attrs: AttributeSet?) :
    Checkable, ThemedFrameLayout(context, attrs) {
    private var filterOption: FeedSourceFilterOption? = null

    private var checkedInternal: Boolean = false
        private set(value) {
            if (value) {
                sourceFilterOptionCheck.setImageResource(R.drawable.sn_ic_check)
            } else {
                sourceFilterOptionCheck.setImageResource(0)
            }
            field = value
            refreshContentDescription()
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.sn_feed_source_filter_option, this)
    }

    fun setFilterOption(option: FeedSourceFilterOption) {
        filterOption = option

        sourceFilterOptionText.text = getLabel(option)

        refreshContentDescription()
    }

    private fun refreshContentDescription() {
        // TODO l18n ... and is there a better way of getting checkbox l18n for free?
        filterOption?.let {
            val checkStatus = if (checkedInternal) "Checked" else "Not checked"
            contentDescription = "$checkStatus filter ${getLabel(it)} checkbox"
        }
    }

    // TODO l18n
    private fun getLabel(filterOption: FeedSourceFilterOption): String =
        context.resources.getString(
            when (filterOption) {
                FeedSourceFilterOption.ALL -> R.string.heading_all_notes
                FeedSourceFilterOption.STICKY_NOTES -> R.string.heading_sticky_notes
                FeedSourceFilterOption.ONENOTE_PAGES -> R.string.heading_all_pages
                FeedSourceFilterOption.SAMSUNG_NOTES -> R.string.heading_samsung_notes
            }
        )

    override fun isChecked(): Boolean = checkedInternal

    override fun toggle() {
        this.checkedInternal = !this.checkedInternal
    }

    override fun setChecked(checked: Boolean) {
        this.checkedInternal = checked
    }
}
