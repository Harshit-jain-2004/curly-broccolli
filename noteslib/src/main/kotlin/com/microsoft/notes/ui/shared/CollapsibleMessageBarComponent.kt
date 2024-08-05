package com.microsoft.notes.ui.shared

import android.content.Context
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.noteslist.NotificationButtonInfo
import kotlinx.android.synthetic.main.sn_collapsible_message_bar.view.*

data class CollapsibleMessageBarErrorMessage(
    val title: String,
    val description: String?,
    val errorIconResId: Int,
    val isExpanded: Boolean,
    val buttonInfo: NotificationButtonInfo?
)

class CollapsibleMessageBarView(context: Context, attributeSet: AttributeSet) :
    ConstraintLayout(context, attributeSet) {

    private var currentError: CollapsibleMessageBarErrorMessage? = null

    // Accessibility Strings
    private val chevronButtonExpandDesc by lazy { resources.getString(R.string.sn_message_bar_expand) }
    private val chevronButtonCollapseDesc by lazy { resources.getString(R.string.sn_message_bar_collapse) }

    private val messageBarClickListener: OnClickListener = OnClickListener {
        currentError?.let {
            if (it.description != null && it.description.isNotEmpty()) {
                TransitionManager.beginDelayedTransition(this@CollapsibleMessageBarView.parent as ViewGroup, ChangeBounds())

                if (it.isExpanded) {
                    hideMessageBody()
                } else {
                    showMessageBody()
                }

                currentError = it.copy(isExpanded = !it.isExpanded)
            }
        }
    }

    fun setError(
        title: String,
        description: String?,
        errorIconOverrideResId: Int,
        buttonInfo: NotificationButtonInfo? = null
    ) {
        currentError = currentError?.copy(
            title = title,
            description = description,
            errorIconResId = errorIconOverrideResId,
            buttonInfo = buttonInfo
        ) ?: CollapsibleMessageBarErrorMessage(
            title = title,
            description = description,
            errorIconResId = errorIconOverrideResId,
            isExpanded = false,
            buttonInfo = buttonInfo
        )
    }

    fun unsetError() {
        currentError = null
    }

    fun show() {
        currentError?.let {
            populateMessageBarComponents(it)
            this.visibility = View.VISIBLE
        }
    }

    fun remove() {
        this.visibility = View.GONE
    }

    private fun populateMessageBarComponents(errorMessage: CollapsibleMessageBarErrorMessage) {
        setErrorTitle(errorMessage.title)

        setErrorIcon(errorMessage.errorIconResId)

        val errorHasDescription = errorMessage.description != null && errorMessage.description.isNotEmpty()
        setChevronButton(isVisible = errorHasDescription)
        setErrorDescription(errorMessage.description)
        setErrorActionButton(errorMessage.buttonInfo)
        collapsibleMessageBarErrorChevron.setOnClickListener(messageBarClickListener)

        if (currentError?.isExpanded == true) {
            showMessageBody()
        } else {
            hideMessageBody()
        }
    }

    private fun setErrorTitle(errorTitleText: String) {
        collapsibleMessageBarErrorTitle?.let {
            if (errorTitleText.isEmpty()) {
                it.visibility = View.GONE
            } else {
                it.text = errorTitleText
                it.visibility = View.VISIBLE
            }
        }
    }

    private fun setErrorIcon(resId: Int) {
        collapsibleMessageBarErrorIcon.setImageResource(resId)
    }

    private fun setChevronButton(isVisible: Boolean) {
        collapsibleMessageBarErrorChevron?.let {
            if (!isVisible) {
                it.visibility = View.GONE
                return
            } else {
                it.visibility = View.VISIBLE
            }
            it.setImageResource(R.drawable.sn_message_bar_chevron_collapsed)
        }
    }

    private fun setErrorDescription(errorDescriptionText: String?) {
        collapsibleMessageBarErrorDescription?.let {
            if (errorDescriptionText.isNullOrEmpty()) {
                it.visibility = View.GONE
            } else {
                it.text = errorDescriptionText
                it.visibility = View.VISIBLE
            }
        }
    }

    private fun setErrorActionButton(buttonInfo: NotificationButtonInfo?) {
        errorActionButton?.let {
            if (buttonInfo == null || buttonInfo.title.isEmpty()) {
                it.visibility = View.GONE
            } else {
                it.text = buttonInfo.title
                it.setOnClickListener { buttonInfo.event.invoke() }
                it.visibility = View.VISIBLE
            }
        }
    }

    private fun hideMessageBody() {
        collapsibleMessageBarErrorBody.visibility = View.GONE
        collapsibleMessageBarErrorChevron.contentDescription = chevronButtonExpandDesc
        collapsibleMessageBarErrorChevron.scaleY = 1f
        adjustLowerPadding(false)
    }

    private fun showMessageBody() {
        collapsibleMessageBarErrorBody.visibility = View.VISIBLE
        collapsibleMessageBarErrorChevron.contentDescription = chevronButtonCollapseDesc
        collapsibleMessageBarErrorChevron.scaleY = -1f
        adjustLowerPadding(true)
    }

    /**
     * This function adjusts the bottom padding for the MessageBar.
     * The criteria are as follows :
     * - If only message header is visible => then no bottom padding is required
     * - If message body is visible => then bottom padding needs to be 12dp
     * @param isMessageBodyVisible
     */
    private fun adjustLowerPadding(isMessageBodyVisible: Boolean) {
        val bottomMarginConstraintSet = ConstraintSet()
        var bottomMarginInPixel = 0

        bottomMarginConstraintSet.clone(this)

        if (isMessageBodyVisible) {
            bottomMarginInPixel = context.resources.getDimension(
                R.dimen.collapsible_message_bar_lower_padding
            ).toInt()
        }
        bottomMarginConstraintSet.setMargin(
            collapsibleMessageBarErrorBody.id, ConstraintSet.BOTTOM,
            bottomMarginInPixel
        )
        bottomMarginConstraintSet.applyTo(this)
    }
}
