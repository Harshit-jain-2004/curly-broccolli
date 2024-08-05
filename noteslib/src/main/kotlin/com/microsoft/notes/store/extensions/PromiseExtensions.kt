package com.microsoft.notes.store.extensions

import com.microsoft.notes.store.Promise
import com.microsoft.notes.store.PromiseImpl

fun <T, U> Promise<T>.map(transform: (T) -> U): Promise<U> {
    val mappedPromise = PromiseImpl<U>()
    this.then {
        mappedPromise.resolve(transform(it))
    }.fail {
        mappedPromise.resolve(it)
    }
    return mappedPromise
}
