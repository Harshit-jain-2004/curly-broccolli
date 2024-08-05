package com.microsoft.notes.ui.noteslist

import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.sideeffect.ui.NoteChanges
import com.microsoft.notes.sideeffect.ui.Notifications
import com.microsoft.notes.sideeffect.ui.SyncStateUpdates
import com.microsoft.notes.sideeffect.ui.UserNotificationsUpdates
import com.microsoft.notes.ui.extensions.filterDeletedAndFutureNotes
import com.microsoft.notes.ui.shared.StickyNotesPresenterWithSyncSpinner
import com.microsoft.notes.ui.shared.SyncStateUpdatingApi

class NotesListPresenter(private val fragmentApi: FragmentApi) :
    StickyNotesPresenterWithSyncSpinner(fragmentApi), Notifications, SyncStateUpdates, NoteChanges, UserNotificationsUpdates {

    // presenter functions

    // ---- Adding new notes ----//

    fun addNote(paragraphText: String?, userID: String, addedNote: (Note) -> Unit) {
        shouldHandleStateUpdates = false

        val promise = NotesLibrary.getInstance().addNote(text = paragraphText, userID = userID)

        promise.then {
            addedNote(it)
        }.fail {
            shouldHandleStateUpdates = true
        }
    }

    fun addInkNote(userID: String, addedNote: (Note) -> Unit) {
        shouldHandleStateUpdates = false

        val promise = NotesLibrary.getInstance().addInkNote(userID = userID)

        promise.then {
            addedNote(it)
        }.fail {
            shouldHandleStateUpdates = true
        }
    }

    // UI bindings
    override fun addUiBindings() {
        try {
            NotesLibrary.getInstance().addUiBindings(this)
        } catch (exception: UninitializedPropertyAccessException) {
            NotesLibrary.getInstance().log(message = "UninitializedPropertyAccessException when adding ui binding")
        }
    }

    override fun removeUiBindings() {
        try {
            NotesLibrary.getInstance().removeUiBindings(this)
        } catch (exception: UninitializedPropertyAccessException) {
            NotesLibrary.getInstance().log(
                message = "UninitializedPropertyAccessException when removing ui binding"
            )
        }
    }

    // --- UI Bindings function listeners ------//

    // NoteChanges
    override fun notesUpdated(stickyNotesCollectionsByUser: HashMap<String, List<Note>>, notesLoaded: Boolean) {
        val stickyNotesCollection: MutableList<Note> = mutableListOf()
        stickyNotesCollectionsByUser.forEach {
            stickyNotesCollection.addAll(it.value)
        }
        runIfActivityIsRunning {
            runOnClientThread {
                fragmentApi.updateNotesCollection(
                    notesCollection = stickyNotesCollection.filterDeletedAndFutureNotes(),
                    scrollTo = ScrollTo.NoScroll,
                    notesLoaded = notesLoaded
                )
            }
        }
    }

    override fun noteDeleted() {}

    // Notifications interface implementation
    override fun upgradeRequired() {}

    override fun syncErrorOccurred(error: Notifications.SyncError, userID: String) {
        runIfActivityIsRunning {
            runOnClientThread {
                val syncErrorStringIds = toSyncErrorStringIds(error)
                syncErrorStringIds?.let { fragmentApi.setSyncStatusMessage(it, userID) }
            }
        }
    }

    override fun updateUserNotifications(userIdToNotificationsMap: Map<String, UserNotifications>) {
        runIfActivityIsRunning {
            runOnClientThread {
                fragmentApi.updateUserNotificationsUi(userIdToNotificationsMap)
            }
        }
    }
}

interface FragmentApi : SyncStateUpdatingApi {
    fun updateNotesCollection(notesCollection: List<Note>, scrollTo: ScrollTo, notesLoaded: Boolean)
    fun updateUserNotificationsUi(userIdToNotificationsMap: Map<String, UserNotifications>)
}

data class SyncErrorResIds(
    val titleId: Int,
    val descriptionId: Int?,
    val errorIconOverrideResId: Int
)
