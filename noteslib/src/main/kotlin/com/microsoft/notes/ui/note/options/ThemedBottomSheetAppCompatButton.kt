package com.microsoft.notes.ui.note.options

import android.content.Context
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import com.microsoft.notes.ui.theme.ThemedAppCompatButton

open class ThemedBottomSheetAppCompatButton(context: Context, attrs: AttributeSet?) : ThemedAppCompatButton(
    context,
    attrs
) {

    // HACK 1 (vineset):- BottomSheet Dialogs are available to show information
    // But here we are inflating layouts in it and displaying button.
    // Hence, we are seeing weird order of views with weird text in accessibility,
    // To avoid this weird order, we are ignoring these calls in BottomSheet buttons
    override fun onPopulateAccessibilityEvent(event: AccessibilityEvent?) {
        // Ignoring the calls to super.
    }
}
