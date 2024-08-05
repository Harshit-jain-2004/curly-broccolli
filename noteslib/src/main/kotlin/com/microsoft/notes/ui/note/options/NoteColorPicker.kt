package com.microsoft.notes.ui.note.options

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableContainer
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.microsoft.notes.models.Color
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.NotesThemeOverride
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.styled.toCardColorContextColor
import com.microsoft.notes.richtext.editor.styled.toIconColorContextColor
import kotlinx.android.synthetic.main.sn_color_items_radio_buttons.view.*

class NoteColorPicker(context: Context, attributeSet: AttributeSet?) : ConstraintLayout(context, attributeSet) {
    interface NoteColorPickerListener {
        fun onColorSelected(color: Color)
    }

    class ChildAlignedLayerDrawable(layerDrawable: LayerDrawable) : LayerDrawable(getDrawables(layerDrawable)) {
        companion object {
            fun getDrawables(layerDrawable: LayerDrawable): Array<Drawable> =
                (0 until layerDrawable.numberOfLayers).map { layerDrawable.getDrawable(it) }.toTypedArray()
        }

        override fun onBoundsChange(bounds: Rect?) {
            super.onBoundsChange(bounds)
            if (bounds == null) {
                return
            }
            // a trick to ensure the center alignment of child drawables
            val backgroundDrawable = getDrawable(0)
            val checkMarkDrawable = getDrawable(1)
            val anchorWidth = backgroundDrawable.intrinsicWidth
            val anchorHeight = backgroundDrawable.intrinsicHeight
            val sizeSource = checkMarkDrawable.run {
                when (this) {
                    is StateListDrawable -> {
                        val constantState = checkMarkDrawable.constantState
                        when (constantState) {
                            is DrawableContainer.DrawableContainerState -> constantState.children.first { it.intrinsicWidth > 0 }
                            else -> null
                        }
                    }
                    else -> null
                }
            } ?: checkMarkDrawable
            val relativeWidth = sizeSource.intrinsicWidth
            val relativeHeight = sizeSource.intrinsicHeight
            val scaleX = bounds.width().toFloat() / anchorWidth
            val scaleY = bounds.height().toFloat() / anchorHeight
            val paddingX = ((anchorWidth - relativeWidth) / 2 * scaleX).toInt()
            val paddingY = ((anchorHeight - relativeHeight) / 2 * scaleY).toInt()
            setLayerInset(1, paddingX, paddingY, paddingX, paddingY)
            checkMarkDrawable.setBounds(bounds.left + paddingX, bounds.top + paddingY, bounds.right - paddingX, bounds.bottom - paddingY)
        }
    }

    constructor(context: Context) : this(context, null)

    private var listener: NoteColorPickerListener? = null

    private var currentColor: Color? = null
    private var themeOverride: NotesThemeOverride.StickyNoteCanvasThemeOverride? = null

    private fun getColorItems() = arrayOf(
        yellow_color_item, green_color_item, pink_color_item, purple_color_item,
        blue_color_item, grey_color_item, charcoal_color_item
    )

    init {
        LayoutInflater.from(context).inflate(R.layout.sn_color_items_radio_buttons, this)

        yellow_color_item.setOnClickListener { onColorItemClicked(it) }
        green_color_item.setOnClickListener { onColorItemClicked(it) }
        pink_color_item.setOnClickListener { onColorItemClicked(it) }
        purple_color_item.setOnClickListener { onColorItemClicked(it) }
        blue_color_item.setOnClickListener { onColorItemClicked(it) }
        grey_color_item.setOnClickListener { onColorItemClicked(it) }
        charcoal_color_item.setOnClickListener {
            onColorItemClicked(it)
            tintCheckMark(charcoal_color_item.background, android.graphics.Color.WHITE)
        }
        themeOverride = NotesLibrary.getInstance().theme.stickyNoteCanvasThemeOverride

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // android:gravity is not supported in LayerDrawable when API level is below 23
            // so we need to apply a custom LayerDrawable to ensure the child alignment
            getColorItems().forEach {
                val drawable = it.background
                if (drawable is LayerDrawable) {
                    it.background = ChildAlignedLayerDrawable(drawable)
                }
            }
        }

        updateAccessibilityLabel()
        setColorItemsBackground()
    }

    private fun updateAccessibilityLabel() {
        getColorItems().forEachIndexed { idx, item ->
            val colorLabel = context.getString(
                when (item) {
                    yellow_color_item -> R.string.sn_change_color_to_yellow
                    green_color_item -> R.string.sn_change_color_to_green
                    pink_color_item -> R.string.sn_change_color_to_pink
                    purple_color_item -> R.string.sn_change_color_to_purple
                    blue_color_item -> R.string.sn_change_color_to_blue
                    grey_color_item -> R.string.sn_change_color_to_grey
                    else -> R.string.sn_change_color_to_charcoal
                }
            )
            val positionLabel = context.getString(R.string.sn_change_color_position, (idx + 1).toString(), getColorItems().size.toString())
            item.contentDescription = "$colorLabel, $positionLabel"
        }
    }

    private fun setColorItemsBackground() {
        /*
         * tintCheckMark function works only above version lollipop.
         * Changing the background colors of drawables to dark mode colors below lollipop will cause accessibility issues
         */
        getColorItems().forEach {
            val drawable = it.background
            if (drawable is LayerDrawable) {
                val backgroundDrawable = drawable.getDrawable(0) as GradientDrawable?
                backgroundDrawable?.setColor(getColorFromItem(it).toCardColorContextColor(context, themeOverride))

                // setting the color and width of item's border
                val strokeWidth = resources.getDimensionPixelSize(R.dimen.sn_color_picker_item_border)
                backgroundDrawable?.setStroke(strokeWidth, getColorFromItem(it).toIconColorContextColor(context, themeOverride))

                radio_group_color_items.clearCheck()
                radio_group_color_items.check(it.id)
                val tintColor = if (themeOverride != null || it == charcoal_color_item) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                tintCheckMark(drawable, tintColor)
            }
        }
        radio_group_color_items.clearCheck()
    }

    fun setListener(listener: NoteColorPickerListener?) {
        this.listener = listener
    }

    private fun tintCheckMark(drawable: Drawable, color: Int) {
        val listDrawable = drawable as? LayerDrawable
        val selector = listDrawable?.getDrawable(1) as? StateListDrawable
        val check = selector?.current
        check?.mutate()
        check?.setTint(color)
    }

    private fun getColorFromItem(view: View): Color {
        return when (view) {
            yellow_color_item -> Color.YELLOW
            green_color_item -> Color.GREEN
            pink_color_item -> Color.PINK
            purple_color_item -> Color.PURPLE
            blue_color_item -> Color.BLUE
            grey_color_item -> Color.GREY
            charcoal_color_item -> Color.CHARCOAL
            else -> Color.getDefault()
        }
    }

    private fun onColorItemClicked(view: View) {
        if (view is AppCompatRadioButton && view.isChecked) {
            val color = getColorFromItem(view)
            if (currentColor != color) {
                currentColor = color
                listener?.onColorSelected(color)
            }
        }
    }

    fun setSelectedColor(color: Color) {
        val viewId = when (color) {
            Color.YELLOW -> yellow_color_item
            Color.GREEN -> green_color_item
            Color.PINK -> pink_color_item
            Color.PURPLE -> purple_color_item
            Color.BLUE -> blue_color_item
            Color.GREY -> grey_color_item
            Color.CHARCOAL -> charcoal_color_item
        }.id
        radio_group_color_items.clearCheck()
        radio_group_color_items.check(viewId)
        tintCheckMark(charcoal_color_item.background, android.graphics.Color.WHITE)
        currentColor = color
    }
}
