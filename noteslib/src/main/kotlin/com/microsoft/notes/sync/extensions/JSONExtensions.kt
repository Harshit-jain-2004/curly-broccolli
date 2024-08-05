package com.microsoft.notes.sync.extensions

import com.microsoft.notes.sync.ApiError
import com.microsoft.notes.sync.ApiResult
import com.microsoft.notes.sync.JSON
import com.microsoft.notes.sync.models.MediaAltTextUpdate
import com.microsoft.notes.sync.models.MediaUpload
import com.microsoft.notes.sync.models.RemoteNote

fun JSON.parseAsRemoteNote(): ApiResult<RemoteNote> {
    val remoteNote = RemoteNote.fromJSON(this)
    return if (remoteNote != null) {
        ApiResult.Success(remoteNote)
    } else {
        ApiResult.Failure(ApiError.InvalidJSONError(this))
    }
}

fun JSON.parseAsUpload(): ApiResult<MediaUpload> {
    val upload = MediaUpload.fromJSON(this)
    return if (upload != null) {
        ApiResult.Success(upload)
    } else {
        ApiResult.Failure(ApiError.InvalidJSONError(this))
    }
}

fun JSON.parseAsMediaAltTextUpdate(): ApiResult<MediaAltTextUpdate> {
    val mediaAltTextUpdate = MediaAltTextUpdate.fromJSON(this)
    return if (mediaAltTextUpdate != null) {
        ApiResult.Success(mediaAltTextUpdate)
    } else {
        ApiResult.Failure(ApiError.InvalidJSONError(this))
    }
}
