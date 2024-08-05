package com.microsoft.notes.sync.extensions

import com.microsoft.notes.sync.ApiError
import com.microsoft.notes.sync.ApiRequestOperation
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation
import com.microsoft.notes.sync.HttpError
import com.microsoft.notes.sync.HttpError400
import com.microsoft.notes.sync.HttpError401
import com.microsoft.notes.sync.HttpError403
import com.microsoft.notes.sync.HttpError404
import com.microsoft.notes.sync.HttpError409
import com.microsoft.notes.sync.HttpError410
import com.microsoft.notes.sync.HttpError413
import com.microsoft.notes.sync.HttpError426
import com.microsoft.notes.sync.HttpError429
import com.microsoft.notes.sync.HttpError500
import com.microsoft.notes.sync.HttpError503
import com.microsoft.notes.sync.UnknownHttpError

fun ApiRequestOperation.toLoggingIdentifier(): String {
    val operationType = when (this) {
        is ValidApiRequestOperation.CreateNote -> "CreateNote"
        is ValidApiRequestOperation.GetNoteForMerge -> "GetNoteForMerge"
        is ValidApiRequestOperation.UpdateNote -> "UpdateNote"
        is ValidApiRequestOperation.DeleteNote -> "DeleteNote"
        is ValidApiRequestOperation.DeleteNoteReference -> "DeleteNoteReference"
        is ValidApiRequestOperation.DeleteSamsungNote -> "DeleteSamsungNote"
        is ValidApiRequestOperation.Sync -> "Sync"
        is ValidApiRequestOperation.NoteReferencesSync -> "NoteReferenceSync"
        is ValidApiRequestOperation.MeetingNotesSync -> "MeetingNotesSync"
        is ValidApiRequestOperation.SamsungNotesSync -> "SamsungNotesSync"
        is ValidApiRequestOperation.UploadMedia -> "UploadMedia"
        is ValidApiRequestOperation.DownloadMedia -> "DownloadMedia"
        is ValidApiRequestOperation.DeleteMedia -> "DeleteMedia"
        is ValidApiRequestOperation.UpdateMediaAltText -> "UpdateMediaAltText"
        is InvalidApiRequestOperation.InvalidUpdateNote -> "InvalidUpdateNote"
        is InvalidApiRequestOperation.InvalidDeleteNote -> "InvalidDeleteNote"
        is InvalidApiRequestOperation.InvalidUploadMedia -> "InvalidUploadMedia"
        is InvalidApiRequestOperation.InvalidDeleteMedia -> "InvalidDeleteMedia"
        is InvalidApiRequestOperation.InvalidUpdateMediaAltText -> "InvalidUpdateMediaAltText"
    }

    return "ApiRequestOperation.$operationType"
}

fun ApiError.toLoggingIdentifier(): String {
    return when (this) {
        is ApiError.NetworkError -> "NetworkError"
        is ApiError.FatalError -> "FatalError"
        is ApiError.InvalidJSONError -> "InvalidJSONError"
        is ApiError.NonJSONError -> "NonJSONError"
        is ApiError.Exception -> "Exception"
        is HttpError -> toLoggingIdentifier()
    }
}

fun HttpError.toLoggingIdentifier(): String {
    return when (this) {
        is HttpError400 -> "HttpError400"
        is HttpError401 -> "HttpError401"
        is HttpError403 -> "HttpError403"
        is HttpError404.Http404RestApiNotFound -> "Http404RestApiNotFound"
        is HttpError404.UnknownHttp404Error -> "UnknownHttp404Error"
        is HttpError409 -> "HttpError409"
        is HttpError410.InvalidSyncToken -> "HttpError410.InvalidSyncToken"
        is HttpError410.InvalidateClientCache -> "HttpError410.InvalidateClientCache"
        is HttpError410.UnknownHttpError410 -> "HttpError410.UnknownHttpError410"
        is HttpError413 -> "HttpError413"
        is HttpError426 -> "HttpError426"
        is HttpError429 -> "HttpError429"
        is HttpError500 -> "HttpError500"
        is HttpError503 -> "HttpError503"
        is UnknownHttpError -> "UnknownHttpError"
    }
}
