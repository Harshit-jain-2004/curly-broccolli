package com.microsoft.notes.store

fun State.getUserStateForUserID(userID: String): UserState =
    userIDToUserStateMap.get(userID) ?: UserState.EMPTY_USER_STATE
