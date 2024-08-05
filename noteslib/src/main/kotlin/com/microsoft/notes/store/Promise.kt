package com.microsoft.notes.store

interface Promise<out T> {

    fun then(callback: (T) -> Unit): Promise<T>

    fun fail(callback: (Exception) -> Unit): Promise<T>
}

internal class PromiseImpl<T> : Promise<T> {

    private val thenCallbacks = mutableListOf<(T) -> Unit>()
    private val failCallbacks = mutableListOf<(Exception) -> Unit>()

    // store the result/error
    // so that callbacks can be executed immediately
    // when then/fail is called after the async task is completed
    private var hasResult = false
    private var resultCache: T? = null
    private var errorCache: Exception? = null

    @Synchronized override fun then(callback: (T) -> Unit): Promise<T> {
        if (hasResult) {
            resultCache?.let {
                callback(it)
            }
        } else {
            thenCallbacks.add(callback)
        }
        return this
    }

    @Synchronized override fun fail(callback: (Exception) -> Unit): Promise<T> {
        val lastError = errorCache
        when (lastError) {
            null -> failCallbacks.add(callback)
            else -> callback(lastError)
        }
        return this
    }

    @Synchronized internal fun resolve(result: T) {
        hasResult = true
        resultCache = result
        thenCallbacks.forEach { it(result) }
    }

    @Synchronized internal fun resolve(error: Exception) {
        errorCache = error
        failCallbacks.forEach { it(error) }
    }
}
