package com.microsoft.notes.utils.threading

import java.io.Serializable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

interface ThreadExecutor : Serializable {
    fun execute(block: () -> Unit)
}

abstract class ThreadExecutorService(open val executorService: ExecutorService) : ThreadExecutor {
    override fun execute(block: () -> Unit) {
        executorService.execute { block() }
    }
}

object ExecutorServices {

    val store: ExecutorService by lazy {
        store()
    }

    private fun store(): ExecutorService =
        Executors.newSingleThreadExecutor(NamedThreadFactory("store"))

    val persistence: ExecutorService by lazy {
        persistence()
    }

    private fun persistence(): ExecutorService =
        Executors.newSingleThreadExecutor(NamedThreadFactory("persistence"))

    val syncPool: ExecutorService by lazy {
        syncPool()
    }

    // the syncPool is not bound by order of operations and should be used for concurrent tasks only
    private fun syncPool(): ExecutorService =
        Executors.newCachedThreadPool(NamedThreadFactory("syncPool"))

    val syncSideEffect: ExecutorService by lazy {
        syncSideEffect()
    }

    private fun syncSideEffect(): ExecutorService =
        Executors.newSingleThreadExecutor(NamedThreadFactory("syncSideEffect"))

    val uiBindings: ExecutorService by lazy {
        uiBindings()
    }

    private fun uiBindings(): ExecutorService =
        Executors.newSingleThreadExecutor(NamedThreadFactory("ui-bindings"))

    val preferencesSideEffect: ExecutorService by lazy {
        preferencesSideEffect()
    }

    private fun preferencesSideEffect(): ExecutorService =
        Executors.newSingleThreadExecutor(NamedThreadFactory("preferencesSideEffect"))

    val systemSideEffect: ExecutorService by lazy {
        systemSideEffect()
    }
    private fun systemSideEffect(): ExecutorService =
        Executors.newSingleThreadExecutor(NamedThreadFactory("systemSideEffect"))

    val serialNoteRefDispatcher: ExecutorService by lazy {
        serialNoteRefDispatcher()
    }

    private fun serialNoteRefDispatcher(): ExecutorService =
        Executors.newSingleThreadExecutor(NamedThreadFactory("serialNoteRefDispatcher"))
}

class NamedThreadFactory(private val name: String) : ThreadFactory {

    private val threadNumber = AtomicInteger(1)

    override fun newThread(runnable: Runnable): Thread = Thread(runnable, "$name  -  ${threadNumber.andIncrement}")
}
