package com.microsoft.notes.sideeffect.persistence

import com.microsoft.notes.models.Note
import com.microsoft.notes.sideeffect.persistence.handler.CreationHandler
import com.microsoft.notes.sideeffect.persistence.handler.DeleteHandler
import com.microsoft.notes.sideeffect.persistence.handler.NoteReferenceHandler
import com.microsoft.notes.sideeffect.persistence.handler.ReadHandler
import com.microsoft.notes.sideeffect.persistence.handler.SamsungNoteHandler
import com.microsoft.notes.sideeffect.persistence.handler.SyncHandler
import com.microsoft.notes.sideeffect.persistence.handler.UpdateHandler
import com.microsoft.notes.store.SideEffect
import com.microsoft.notes.store.State
import com.microsoft.notes.store.Store
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.AuthAction
import com.microsoft.notes.store.action.CompoundAction
import com.microsoft.notes.store.action.CreationAction
import com.microsoft.notes.store.action.DeleteAction
import com.microsoft.notes.store.action.NoteReferenceAction
import com.microsoft.notes.store.action.ReadAction
import com.microsoft.notes.store.action.SamsungNotesResponseAction
import com.microsoft.notes.store.action.SyncResponseAction
import com.microsoft.notes.store.action.UpdateAction
import com.microsoft.notes.store.getNoteForNoteLocalId
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.threading.ExecutorServices
import com.microsoft.notes.utils.threading.ThreadExecutor
import com.microsoft.notes.utils.threading.ThreadExecutorService

class PersistenceThreadService : ThreadExecutorService(
    ExecutorServices.persistence
)

class PersistenceSideEffect(
    val store: Store,
    val persistenceThread: ThreadExecutor? = null,
    val notesDatabaseManager: NotesDatabaseManager,
    val notesLogger: NotesLogger? = null
) :
    SideEffect(persistenceThread) {

    override fun handle(action: Action, state: State) {
        val findNote: (String) -> Note? = { noteId -> state.getNoteForNoteLocalId(noteId) }
        when (action) {
            is CompoundAction -> action.actions.forEach { handle(it, state) }
            is CreationAction -> notesDatabaseManager.getNotesDatabaseForUser(action.userID)?.let {
                    notesDB ->
                CreationHandler.handle(action, notesDB, notesLogger, findNote, {
                    store.dispatch(it)
                })
            }
            is UpdateAction -> notesDatabaseManager.getNotesDatabaseForUser(action.userID)?.let {
                    notesDB ->
                UpdateHandler.handle(action, notesDB, notesLogger, findNote, {
                    store.dispatch(it)
                })
            }
            is DeleteAction -> notesDatabaseManager.getNotesDatabaseForUser(action.userID)?.let {
                    notesDB ->
                DeleteHandler.handle(action, notesDB, notesLogger, findNote, {
                    store.dispatch(it)
                })
            }
            is ReadAction -> notesDatabaseManager.getNotesDatabaseForUser(action.userID)?.let {
                    notesDB ->
                ReadHandler.handle(action, notesDB, notesLogger, findNote, { store.dispatch(it) })
            }
            is SyncResponseAction -> notesDatabaseManager.getNotesDatabaseForUser(action.userID)?.let {
                    notesDB ->
                SyncHandler.handle(action, notesDB, notesLogger, findNote, { store.dispatch(it) })
            }
            is AuthAction.LogoutAction -> notesDatabaseManager.handleLogout(action.userID)
            is AuthAction.NewAuthTokenAction -> {
                handleNewAuthAction(action)
            }
            is NoteReferenceAction -> notesDatabaseManager.getNotesDatabaseForUser(action.userID)?.let {
                    notesDB ->
                NoteReferenceHandler.handle(action, notesDB, notesLogger, findNote, {
                    store
                        .dispatch(it)
                })
            }
            is SamsungNotesResponseAction -> notesDatabaseManager.getNotesDatabaseForUser(action.userID)?.let {
                    notesDB ->
                SamsungNoteHandler.handle(action, notesDB, notesLogger, findNote, {
                    store
                        .dispatch(it)
                })
            }
        }
    }

    private fun handleNewAuthAction(action: AuthAction.NewAuthTokenAction) {
        val databaseName: String = notesDatabaseManager.handleNewUser(action.userInfo)
        store.dispatch(
            AuthAction.AccountInfoForIntuneProtection(
                databaseName = databaseName,
                userID =
                action.userID,
                accountType = action.userInfo.accountType
            )
        )
    }
}
