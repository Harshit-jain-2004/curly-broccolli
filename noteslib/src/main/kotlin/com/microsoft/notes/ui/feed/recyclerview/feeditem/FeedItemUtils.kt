package com.microsoft.notes.ui.feed.recyclerview.feeditem

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.theme.ThemedCardView

fun setCheckBoxBackground(drawable: Drawable, context: Context, isThemeOverride: Boolean) {
    DrawableCompat.setTint(
        drawable,
        ContextCompat.getColor(
            context,
            if (!isThemeOverride) R.color.sn_checkbox_background
            else R.color.sn_checkbox_background_dark
        )
    )
}

fun handleActionModeBorder(
    isChecked: Boolean,
    isFeedUIRefreshEnabled: Boolean,
    noteLayout: View,
    isThemeOverride: Boolean
) {
    if (isFeedUIRefreshEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (isChecked) {
            noteLayout.foreground = ContextCompat.getDrawable(
                noteLayout.context,
                if (!isThemeOverride) R.drawable.action_mode_feed_items_border
                else R.drawable.action_mode_feed_items_border_dark
            )
        } else {
            noteLayout.foreground = null
        }
    }
}

fun handleCheckBoxVisibility(
    isVisible: Boolean,
    isChecked: Boolean,
    actionModeCheckBox: AppCompatCheckBox?,
    cardView: ThemedCardView,
    isThemeOverride: Boolean
) {

    val checkBoxVisibility = if (isVisible) View.VISIBLE else View.GONE
    if (actionModeCheckBox?.visibility != checkBoxVisibility) {
        actionModeCheckBox?.visibility = checkBoxVisibility
        if (checkBoxVisibility == View.GONE) {
            cardView.requestLayout()
        }
    }

    if (actionModeCheckBox?.isChecked != isChecked) {
        actionModeCheckBox?.isChecked = isChecked
        if (isChecked) {
            if (NotesLibrary.getInstance().experimentFeatureFlags.multiSelectInActionModeEnabled) {
                actionModeCheckBox?.buttonDrawable = cardView.resources.getDrawable(R.drawable.sn_feed_selected_checkmark_circle)
            } else {
                actionModeCheckBox?.buttonDrawable = cardView.resources.getDrawable(R.drawable.sn_feed_radio_button_selected)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                actionModeCheckBox?.buttonDrawable?.let {
                    DrawableCompat.setTint(it, ContextCompat.getColor(cardView.context, NotesLibrary.getInstance().theme.primaryAppColor))
                }
            }
        } else {
            actionModeCheckBox?.buttonDrawable = cardView.resources.getDrawable(R.drawable.sn_feed_radio_button_unselected)
        }
        actionModeCheckBox?.background?.let { setCheckBoxBackground(it, cardView.context, isThemeOverride) }
    }
}

fun setContentLayoutBackgroundColor(
    cardBodyColor: Int,
    cardBorderColor: Int,
    noteLayout: View,
    isFeedUIRefreshEnabled: Boolean,
    isSamsungNote: Boolean
) {

    val context = noteLayout.context
    val resources = noteLayout.resources

    val layerDrawable = ContextCompat.getDrawable(
        context,
        getContentBackground(isSamsungNote)
    ) as LayerDrawable
    val colorBG = layerDrawable.findDrawableByLayerId(
        getContextBgColor(isSamsungNote)
    ) as GradientDrawable
    colorBG.setColor(cardBodyColor)
    colorBG.cornerRadius = resources.getDimension(R.dimen.sn_note_card_view_radius)

    val noteBorder = ContextCompat.getDrawable(
        context,
        R.drawable.sn_note_item_border
    ) as GradientDrawable
    noteBorder.mutate()

    val borderWidth = resources.getDimensionPixelSize(R.dimen.feed_card_border_stroke_width)

    if (!isFeedUIRefreshEnabled)
        noteBorder.setStroke(borderWidth, cardBorderColor)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        layerDrawable.addLayer(noteBorder)
    }

    noteLayout.background = layerDrawable
}

fun getContentBackground(isSamsungNotes: Boolean) =
    if (isSamsungNotes) R.drawable.samsung_card_content_background else R.drawable.sn_card_content_background

fun getContextBgColor(isSamsungNote: Boolean) =
    if (isSamsungNote) R.id.samsung_card_content_bg_color else R.id.card_content_bg_color
