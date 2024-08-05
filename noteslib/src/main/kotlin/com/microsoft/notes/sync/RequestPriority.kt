package com.microsoft.notes.sync

// We are suppressing this because "foreground" and "background" are recognized by Fabric Service.
@Suppress("EnumNaming")
enum class RequestPriority {
    foreground,
    background
}
