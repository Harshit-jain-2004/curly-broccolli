package com.microsoft.notes.store.action

import com.microsoft.notes.models.Changes
import com.microsoft.notes.utils.utils.toNullabilityIdentifierString

sealed class SamsungNotesResponseAction(val userID: String) : Action {

    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is ApplyChanges -> "SamsungNotesApplyChanges"
        }

        return "SamsungNote.$actionType"
    }

    class ApplyChanges(val changes: Changes, userID: String, val deltaToken: String?) : SamsungNotesResponseAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: changes = $changes, deltaToken = ${toNullabilityIdentifierString(deltaToken)}"
    }
}
