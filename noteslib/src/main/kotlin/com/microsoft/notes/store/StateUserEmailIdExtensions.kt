package com.microsoft.notes.store

fun State.withEmailForUser(emailID: String, userID: String): State {
    val newUserState = getUserStateForUserID(userID).copy(email = emailID)
    return newState(userID = userID, updatedUserState = newUserState)
}
