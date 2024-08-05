package com.microsoft.notes.ui.search

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.CheckBox
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import com.microsoft.notes.models.Color
import com.microsoft.notes.noteslib.R
import kotlinx.android.synthetic.main.sn_color_items_search.view.*

class SearchColorPicker(context: Context, attributeSet: AttributeSet?) : ConstraintLayout(context, attributeSet) {
    interface NoteColorPickerListener {
        fun onColorSelected(color: Color?)
    }

    constructor(context: Context) : this(context, null)

    private var listener: NoteColorPickerListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.sn_color_items_search, this)

        val onClickListener = OnClickListener { view -> onColorItemClicked(view as AppCompatCheckBox, view.isChecked) }
        yellow_color_item.setOnClickListener(onClickListener)
        green_color_item.setOnClickListener(onClickListener)
        pink_color_item.setOnClickListener(onClickListener)
        purple_color_item.setOnClickListener(onClickListener)
        blue_color_item.setOnClickListener(onClickListener)
        grey_color_item.setOnClickListener(onClickListener)
        charcoal_color_item.setOnClickListener { view ->
            onClickListener.onClick(view)
            tintCharcoalCheckMark(view.background)
        }
    }

    fun setListener(listener: NoteColorPickerListener?) {
        this.listener = listener
    }

    private fun tintCharcoalCheckMark(drawable: Drawable) {
        val listDrawable = drawable as? LayerDrawable
        val selector = listDrawable?.getDrawable(1) as? StateListDrawable
        val check = selector?.current
        check?.mutate()
        check?.setTint(android.graphics.Color.WHITE)
    }

    private fun onColorItemClicked(view: View, isChecked: Boolean) {
        var color: Color? = null
        if (isChecked) {
            color = when (view) {
                yellow_color_item -> Color.YELLOW
                green_color_item -> Color.GREEN
                pink_color_item -> Color.PINK
                purple_color_item -> Color.PURPLE
                blue_color_item -> Color.BLUE
                grey_color_item -> Color.GREY
                charcoal_color_item -> Color.CHARCOAL
                else -> null
            }
        }
        if (color != null) {
            setSelectedColor(color)
        } else {
            clearColorSelection()
        }
        listener?.onColorSelected(color)
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
        clearColorSelection()
        findViewById<CheckBox>(viewId).isChecked = true
        tintCharcoalCheckMark(charcoal_color_item.background)
    }

    fun clearColorSelection() {
        yellow_color_item.isChecked = false
        green_color_item.isChecked = false
        pink_color_item.isChecked = false
        purple_color_item.isChecked = false
        blue_color_item.isChecked = false
        grey_color_item.isChecked = false
        charcoal_color_item.isChecked = false
    }
}
