package com.microsoft.notes.utils.logging

interface Logger {
    fun d(tag: String, message: String, exception: Throwable? = null)
    fun v(tag: String, message: String, exception: Throwable? = null)
    fun e(tag: String, message: String, exception: Throwable? = null)
    fun i(tag: String, message: String, exception: Throwable? = null)
    fun w(tag: String, message: String, exception: Throwable? = null)
}
