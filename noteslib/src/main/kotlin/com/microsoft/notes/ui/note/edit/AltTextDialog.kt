package com.microsoft.notes.ui.note.edit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.style.MetricAffectingSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
import androidx.fragment.app.DialogFragment
import com.microsoft.notes.noteslib.R
import kotlinx.android.synthetic.main.sn_alt_text_dialog.*

internal class AltTextDialog : DialogFragment() {
    interface Listener {
        fun onAltTextChanged(altText: String)
    }

    companion object {
        private const val ALT_TEXT = "ALT_TEXT"

        fun createDialog(altText: String?): AltTextDialog {
            val frag = AltTextDialog()
            val args = Bundle()
            args.putString(ALT_TEXT, altText)
            frag.arguments = args
            return frag
        }
    }

    internal var listener: Listener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.sn_alt_text_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        altTextDialogEditText.setText(arguments?.getString(ALT_TEXT, "") ?: "")
        altTextDialogSave.setOnClickListener {
            listener?.onAltTextChanged(altTextDialogEditText.text.toString())
            dismiss()
        }
        altTextDialogCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // show keyboard when dialog is opened
        altTextDialogEditText.requestFocus()
        altTextDialogEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(string: Editable?) {
                string?.let {
                    it.getSpans(0, it.length, MetricAffectingSpan::class.java)
                        .forEach { span -> string.removeSpan(span) }
                }
            }
        })
        dialog?.window?.setSoftInputMode(SOFT_INPUT_STATE_VISIBLE)

        // increase the default width of the dialog
        val layoutParams = dialog?.window?.attributes
        layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
        dialog?.window?.attributes = layoutParams
    }
}
