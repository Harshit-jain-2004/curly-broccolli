package com.microsoft.notes.noteslib

interface FeedAuthProvider {
    fun getAuthTokenForResource(userID: String, resource: String, onSuccess: (token: String) -> Unit, onFailure: () -> Unit)
}
