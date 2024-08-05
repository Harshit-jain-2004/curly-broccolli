package com.microsoft.notes.store.action

import com.microsoft.notes.models.Note
import com.microsoft.notes.ui.feed.filter.FeedFilters
import com.microsoft.notes.ui.feed.recyclerview.FeedLayoutType
import com.microsoft.notes.ui.feed.sourcefilter.FeedSourceFilterOption

sealed class UIAction : Action {

    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is AddNewNote -> "AddNewNote"
            is EditNote -> "EditNote"
            is EditSearchNote -> "EditSearchNote"
            is EditFeedSearchNote -> "EditFeedSearchNote"
            is SwipeToRefreshStarted -> "SwipeToRefreshStarted"
            is SwipeToRefreshCompleted -> "SwipeToRefreshCompleted"
            is FeedSwipeToRefreshStarted -> "FeedSwipeToRefreshStarted"
            is NoteOptionsDismissed -> "NoteOptionsDismissed"
            is NoteOptionsSendFeedback -> "NoteOptionsSendFeedback"
            is NoteOptionsNoteDeleted -> "NoteOptionsNoteDeleted"
            is NoteOptionsColorPicked -> "NoteOptionsColorPicked"
            is NoteOptionsSearchInNote -> "NoteOptionsSearchInNote"
            is NoteOptionsNoteShared -> "NoteOptionsNoteShared"
            is AddPhotoAction -> "AddPhotoAction"
            is CaptureNoteAction -> "CaptureNoteAction"
            is OnMicroPhoneButtonClickedAction -> "OnMicroPhoneButtonClickedAction"
            is OnScanButtonClickedAction -> "OnScanButtonClickedAction"
            is ClearCanvasAction -> "ClearCanvasAction"
            is ImageCompressionCompleted -> "ImageCompressionCompleted"
            is NoteFirstEdited -> "NoteFirstEdited"
            is AccountChanged -> "AccountChanged"
            is UpdateCurrentUserID -> "UpdateCurrentUserID"
            is UpdateFutureNoteUserNotification -> "UpdateFutureNoteUserNotification"
            is FeedSourceFilterSelected -> "FeedSourceFilterSelected"
            is FeedNoteOrganiseAction -> "FeedNoteOrganiseAction"
            is FinishActionModeOnFeed -> "FinishActionModeOnFeed"
            is InvalidateActionModeOnFeed -> "InvalidateActionModeOnFeed"
            is ChangeFeedLayout -> "ChangeFeedLayout"
            is RequestClientAuthAction -> "RequestClientAuthAction"
            is ComprehensiveFeedSourceFilterSelected -> "ComprehensiveFeedSourceFilterSelected"
            is DisplayFilterAndSortPanel -> "DisplayFilterAndSortPanel"
            is DeletedMultipleNotes -> "DeletedMultipleNotes"
            is UndoRedoInNotesEditText -> "UndoRedoInNotesEditText"
        }

        return "UIAction.$actionType"
    }

    class AddNewNote(val note: Note) : UIAction()
    class EditNote(val note: Note) : UIAction()
    class EditSearchNote(val note: Note) : UIAction()
    class EditFeedSearchNote(val note: Note) : UIAction()
    class SwipeToRefreshStarted : UIAction()
    class SwipeToRefreshCompleted : UIAction()
    class FeedSwipeToRefreshStarted : UIAction()
    class NoteOptionsDismissed : UIAction()
    class NoteOptionsSendFeedback : UIAction()
    class NoteOptionsNoteDeleted : UIAction()
    class NoteOptionsColorPicked : UIAction()
    class NoteOptionsSearchInNote : UIAction()
    class NoteOptionsNoteShared : UIAction()
    class AddPhotoAction : UIAction()
    class CaptureNoteAction : UIAction()
    class OnMicroPhoneButtonClickedAction : UIAction()
    class OnScanButtonClickedAction : UIAction()
    class ClearCanvasAction : UIAction()
    class ImageCompressionCompleted(val successful: Boolean) : UIAction()
    class NoteFirstEdited : UIAction()
    data class AccountChanged(val userID: String) : UIAction()
    class UpdateCurrentUserID(val userID: String) : UIAction()
    class UpdateFutureNoteUserNotification(val notes: List<Note>, val userID: String) : UIAction()
    class RequestClientAuthAction(val userID: String) : UIAction()

    // Feed
    class FeedSourceFilterSelected(val source: FeedSourceFilterOption) : UIAction()
    class ComprehensiveFeedSourceFilterSelected(val feedFilters: FeedFilters, val scrollToTop: Boolean) : UIAction()
    class FeedNoteOrganiseAction(val note: Note) : UIAction()
    class FinishActionModeOnFeed : UIAction()
    class InvalidateActionModeOnFeed : UIAction()
    class ChangeFeedLayout(val layoutType: FeedLayoutType) : UIAction()
    class DisplayFilterAndSortPanel : UIAction()
    class DeletedMultipleNotes : UIAction()
    class UndoRedoInNotesEditText(val isRedoAction: Boolean) : UIAction()
}
