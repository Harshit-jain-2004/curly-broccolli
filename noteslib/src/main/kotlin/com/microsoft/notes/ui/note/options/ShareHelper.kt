package com.microsoft.notes.ui.note.options

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.styled.shouldRenderAsHTML
import com.microsoft.notes.richtext.scheme.asString
import com.microsoft.notes.richtext.scheme.getSpannedFromHtml
import com.microsoft.notes.sideeffect.sync.getSamsungAttachedImagesForHTMLNote
import com.microsoft.notes.sideeffect.sync.getSamsungMediaForPreviewImage
import com.microsoft.notes.ui.extensions.getHasImagesTelemetryValue
import com.microsoft.notes.ui.extensions.getLocalMediaUrls
import com.microsoft.notes.ui.extensions.isSamsungNote
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.GenericError
import com.microsoft.notes.utils.logging.HostTelemetryKeys
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.microsoft.notes.utils.utils.Constants
import java.io.File
import java.lang.ref.WeakReference
import java.net.URI

/**
 * Existing share (as pdf and text) option has been renamed to export
 * todo rename share to export in code files
 * priority low
 */
fun shareNote(note: Note, weakReferenceActivity: WeakReference<Activity>) {
    shareNoteInternal(note, weakReferenceActivity)
}

fun shareNote(
    note: Note,
    weakReferenceActivity: WeakReference<Activity>,
    shareIntentHandler: (context: Context, intent: Intent) -> Unit
) {
    shareNoteInternal(note, weakReferenceActivity, shareIntentHandler = shareIntentHandler)
}

private fun shareNoteInternal(
    note: Note,
    weakReferenceActivity: WeakReference<Activity>,
    shareIntentHandler: ((context: Context, intent: Intent) -> Unit)? = null
) {
    weakReferenceActivity.get()?.let { activity ->
        val document = note.document

        val intentBuilder = ShareCompat.IntentBuilder.from(activity)

        setText(intentBuilder, note, activity)
        setImagesAndType(intentBuilder, getMediaUrlsToShare(note), activity)
        setTitle(intentBuilder, activity, note)

        var errorMessage = ""
        try {
            if (shareIntentHandler != null) {
                shareIntentHandler(activity, intentBuilder.intent)
            } else {
                intentBuilder.startChooser()
            }
        } catch (e: ActivityNotFoundException) {
            errorMessage = e.message ?: GenericError.UNKNOWN_ERROR.name
        } finally {
            if (errorMessage.isEmpty()) {
                NotesLibrary.getInstance().recordTelemetry(
                    EventMarkers.ShareNoteSuccessful,
                    Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES, note.getHasImagesTelemetryValue())
                )
            } else {
                NotesLibrary.getInstance().recordTelemetry(
                    EventMarkers.ShareNoteFailed,
                    Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES, note.getHasImagesTelemetryValue()),
                    Pair(HostTelemetryKeys.ERROR_MESSAGE, errorMessage)
                )
            }
        }
    } ?: run {
        NotesLibrary.getInstance().recordTelemetry(
            EventMarkers.ShareNoteFailed,
            Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES, note.getHasImagesTelemetryValue()),
            Pair(HostTelemetryKeys.ERROR_MESSAGE, "ActivityIsNull")
        )
    }
}

private fun setText(intentBuilder: ShareCompat.IntentBuilder, note: Note, activity: Activity) {
    val document = note.document
    val shareReferralStringToAppend = "\n\n\n" + activity.resources.getString(R.string.share_referral_message) + ": " + if (NotesLibrary.getInstance().experimentFeatureFlags.appendAndroidAppInstallLinkInShareFlowEnabled)
        Constants.ANDROID_APP_INSTALL_LINK_VIA_SHARE
    else
        Constants.APP_INSTALL_LINK_VIA_SHARE
    val isShareReferralEnabled = NotesLibrary.getInstance().experimentFeatureFlags.attachAppInstallLinkInShareEnabled
    if (document.isSamsungNoteDocument) {
        if (shouldRenderAsHTML(note)) {
            val textHtmlBody = document.body.replace("<img.+?>".toRegex(), "")
            val title = note.title
            val body = getSpannedFromHtml(textHtmlBody).trim()
            if (title.isNullOrEmpty()) {
                intentBuilder.setText(if (isShareReferralEnabled) body.toString() + shareReferralStringToAppend else body)
            } else if (body.isEmpty()) {
                intentBuilder.setText(if (isShareReferralEnabled) title.trim() + shareReferralStringToAppend else title)
            } else {
                intentBuilder.setText(if (isShareReferralEnabled) title.trim() + "\n" + body + shareReferralStringToAppend else title + "\n" + body)
            }
        } else {
            intentBuilder.setText(if (isShareReferralEnabled) note.title?.trim() + shareReferralStringToAppend else note.title)
        }
    } else {
        intentBuilder.setText(if (isShareReferralEnabled) document.asString().trim() + shareReferralStringToAppend else document.asString())
    }
}

private fun getMediaUrlsToShare(note: Note): List<String> {
    val document = note.document
    return if (document.isSamsungNoteDocument) {
        if (shouldRenderAsHTML(note)) {
            note.media.getSamsungAttachedImagesForHTMLNote().asSequence()
                .filter { !it.localUrl.isNullOrBlank() }
                .mapNotNull { it.localUrl }
                .toList()
        } else {
            val previewUrl = note.media.getSamsungMediaForPreviewImage()?.localUrl
            if (!previewUrl.isNullOrBlank()) {
                listOf(previewUrl.toString())
            } else {
                emptyList()
            }
        }
    } else {
        note.getLocalMediaUrls()
    }
}

private fun setImagesAndType(
    intentBuilder: ShareCompat.IntentBuilder,
    imagesUrls: List<String>,
    activity: Activity
) {
    imagesUrls.forEach {
        intentBuilder.addStream(NotesLibrary.getInstance().contentUri(activity, it))
    }

    if (imagesUrls.isEmpty()) {
        intentBuilder.setType("text/plain")
    } else {
        intentBuilder.setType("image/*")
    }
}

private fun setTitle(intentBuilder: ShareCompat.IntentBuilder, activity: Activity, note: Note) {
    val chooserTitle = if (note.isSamsungNote()) {
        activity.resources.getString(R.string.samsung_note_share_dialog_title)
    } else {
        activity.resources.getString(R.string.sn_share_dialog_title)
    }
    intentBuilder.setChooserTitle(chooserTitle)
}

fun createContentUri(context: Context, imageUrl: String): Uri =
    FileProvider.getUriForFile(context, context.packageName, File(URI.create(imageUrl)))
