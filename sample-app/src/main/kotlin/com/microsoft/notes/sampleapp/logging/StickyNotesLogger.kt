package com.microsoft.notes.sampleapp.logging

import android.util.Log
import com.microsoft.notes.utils.logging.Logger

class StickyNotesLogger : Logger {
    override fun d(tag: String, message: String, exception: Throwable?) {
        Log.d(tag, message, exception)
    }

    override fun v(tag: String, message: String, exception: Throwable?) {
        Log.v(tag, message, exception)
    }

    override fun e(tag: String, message: String, exception: Throwable?) {
        Log.e(tag, message, exception)
    }

    override fun i(tag: String, message: String, exception: Throwable?) {
        Log.i(tag, message, exception)
    }

    override fun w(tag: String, message: String, exception: Throwable?) {
        Log.w(tag, message, exception)
    }
}