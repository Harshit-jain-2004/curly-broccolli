package com.microsoft.notes.ui.noteslist.placeholder

import android.content.Context
import android.os.Build
import android.text.SpannableString
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.theme.ThemedLinearLayout
import kotlinx.android.synthetic.main.sn_notes_list_placeholder_layout.view.*

internal class NotesListPlaceholder(context: Context, attributeSet: AttributeSet?) :
    ThemedLinearLayout(context, attributeSet) {
    constructor(context: Context) : this(context, null)

    private var image: Int? = null
    private var imageContentDescription: String? = null
    private var title: SpannableString? = null
    private var titleContentDescription: String? = null
    private var titleStyle: Int? = null
    private var subtitle: SpannableString? = null
    private var subtitleContentDescription: String? = null
    private var subtitleStyle: Int? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.sn_notes_list_placeholder_layout, this)
    }

    internal fun setPlaceholder(
        image: Int? = null,
        imageContentDescription: String? = null,
        title: SpannableString? = null,
        titleContentDescription: String? = null,
        titleStyle: Int? = null,
        subtitle: SpannableString? = null,
        subtitleContentDescription: String? = null,
        subtitleStyle: Int? = null
    ) {
        this.image = image
        this.imageContentDescription = imageContentDescription
        this.title = title
        this.titleContentDescription = titleContentDescription
        this.titleStyle = titleStyle
        this.subtitle = subtitle
        this.subtitleContentDescription = subtitleContentDescription
        this.subtitleStyle = subtitleStyle
    }

    internal fun showPlaceholder() {
        if (image == null && title == null && subtitle == null) {
            visibility = View.GONE
            return
        }

        showImage()
        showText(notesPlaceholderTitle, title, titleContentDescription, titleStyle)
        showText(notesPlaceholderSubtitle, subtitle, subtitleContentDescription, subtitleStyle)
        visibility = View.VISIBLE
    }

    private fun showImage() {
        if (image == null || imageContentDescription == null) {
            notesPlaceholderImage.visibility = View.GONE
            return
        }

        image?.let { notesPlaceholderImage.setImageResource(it) }
        if (!imageContentDescription.isNullOrEmpty()) {
            notesPlaceholderImage.contentDescription = imageContentDescription
            notesPlaceholderImage.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        } else {
            notesPlaceholderImage.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        notesPlaceholderImage.visibility = View.VISIBLE
    }

    private fun showText(textView: TextView, text: SpannableString?, contentDescription: String?, style: Int?) {
        if (text.isNullOrEmpty()) {
            textView.visibility = View.GONE
            return
        }

        textView.text = text
        if (!contentDescription.isNullOrEmpty()) {
            textView.contentDescription = contentDescription
        }
        style?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textView.setTextAppearance(it)
            } else {
                // deprecated from API 23, only using for API < 23
                @Suppress("DEPRECATION")
                textView.setTextAppearance(context, it)
            }
        }
        textView.visibility = View.VISIBLE
    }
}
