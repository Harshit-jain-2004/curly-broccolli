package com.microsoft.notes.sync

import org.jdeferred.impl.DeferredObject
import java.lang.IllegalStateException

fun <T> (() -> ApiPromise<T>).withBackoff(
    backoffStrategy: BackoffStrategy,
    shouldRetry: ((failure: ApiResult.Failure<T>) -> Boolean)? = null
): ApiPromise<T> {
    val deferred = DeferredObject<ApiResult<T>, Exception, Any>()
    lateinit var lastFailure: ApiResult.Failure<T>
    var firstAttempt = true
    do {
        if (firstAttempt) {
            firstAttempt = false
        } else {
            ApiPromise.delay(backoffStrategy.getNextDelayInMs()).waitForPromise()
        }
        val promise = this()
        promise.onComplete {
            when (it) {
                is ApiResult.Success -> deferred.resolve(it)
                is ApiResult.Failure -> lastFailure = it
            }
        }
        promise.waitForPromise()
        if (deferred.isResolved) {
            return ApiPromise(deferred.promise())
        } else if (shouldRetry != null && !shouldRetry(lastFailure)) {
            return ApiPromise.of(lastFailure)
        }
    } while (backoffStrategy.shouldRetry())

    return ApiPromise.of(lastFailure)
}

interface BackoffStrategy {
    fun shouldRetry(): Boolean
    fun getNextDelayInMs(): Long
}

class ExponentialBackoff(
    private var retryTimes: Int = 3,
    initialDelayInMs: Long = 500,
    private val maxDelayInMs: Long = 2000,
    private val factor: Double = 2.0
) : BackoffStrategy {
    private var currentDelayInMs = initialDelayInMs

    override fun shouldRetry(): Boolean = retryTimes > 0

    override fun getNextDelayInMs(): Long {
        if (!shouldRetry()) {
            throw IllegalStateException("Called getNextDelayInMs when shouldRetry is false")
        }
        val nextDelayInMs = currentDelayInMs
        currentDelayInMs = (currentDelayInMs * factor).toLong().coerceAtMost(maxDelayInMs)
        retryTimes -= 1
        return nextDelayInMs
    }
}
