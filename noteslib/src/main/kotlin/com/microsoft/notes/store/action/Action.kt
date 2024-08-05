package com.microsoft.notes.store.action

interface Action {
    fun toLoggingIdentifier(): String
    fun toPIIFreeString(): String = toLoggingIdentifier()
}

class CompoundAction(vararg val actions: Action) : Action {
    override fun toLoggingIdentifier(): String {
        var identifier = "Compound Action {"
        actions.forEach {
            identifier += "${it.toLoggingIdentifier()}, "
        }
        identifier += "}"

        return identifier
    }
}
