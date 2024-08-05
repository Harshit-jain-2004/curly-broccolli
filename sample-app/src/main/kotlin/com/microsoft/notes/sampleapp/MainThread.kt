package com.microsoft.notes.sampleapp

import android.content.Context
import android.os.Handler
import com.microsoft.notes.utils.threading.ThreadExecutor

//TODO We don't want to force clients to do that, we should get rid of the clientThread on our library
//or instead allow a more flexible option to define a thread, like a lambda.
class MainThread(private val context: Context) : ThreadExecutor {
    override fun execute(block: () -> Unit) {
        val handler = Handler(context.mainLooper)
        handler.post { block.invoke() }
    }
}