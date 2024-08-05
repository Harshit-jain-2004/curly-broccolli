// `this` is used safely in the initializer block. We need to call the register function in this way to ensure it
// gets registered before the view gets attached
@file:Suppress("LeakingThis")

package com.microsoft.notes.ui.theme

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.microsoft.notes.noteslib.NotesLibrary

open class ThemedAppCompatImageButton(context: Context, attrs: AttributeSet?) : AppCompatImageButton(
    context,
    attrs
) {
    init { registerAndSetTheme(this) }
}

open class ThemedAppCompatButton(context: Context, attrs: AttributeSet?) : AppCompatButton(context, attrs) {
    init { registerAndSetTheme(this) }
}

open class ThemedFrameLayout(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    init { registerAndSetTheme(this) }
}

open class ThemedConstraintLayout(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    init { registerAndSetTheme(this) }
}

open class ThemedLinearLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    init { registerAndSetTheme(this) }
}

open class ThemedRecyclerView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {
    init { registerAndSetTheme(this) }
}

open class ThemedCardView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    init { registerAndSetTheme(this) }
}

open class ThemedImageView(context: Context, attrs: AttributeSet?) : ImageView(context, attrs) {
    init { registerAndSetTheme(this) }
}

open class ThemedTextView(context: Context, attrs: AttributeSet?) : TextView(context, attrs) {
    init { registerAndSetTheme(this) }
}

open class ThemedSwipeToRefresh(context: Context, attrs: AttributeSet?) : SwipeRefreshLayout(context, attrs) {
    init { registerAndSetTheme(this) }
}

open class ThemedDividerView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    init { registerAndSetTheme(this) }
}

private fun registerAndSetTheme(view: View) {
    NotesLibrary.getInstance().registerViewForTheming(view)
}
