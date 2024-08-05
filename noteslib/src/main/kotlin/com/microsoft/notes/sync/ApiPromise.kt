package com.microsoft.notes.sync

import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject

sealed class ApiResult<out T> {
    data class Success<out T>(val value: T) : ApiResult<T>()
    data class Failure<out T>(val error: ApiError) : ApiResult<T>()

    fun unwrap(): T? {
        return when (this) {
            is Success -> this.value
            else -> null
        }
    }
}

class ApiPromise<out T>(private val promise: Promise<ApiResult<T>, Exception, Any>) {
    companion object {
        /**
         * Run a synchronous block on a separate thread
         *
         * @param[runner] the block to execute
         */
        fun <T> task(runner: () -> T): ApiPromise<T> {
            return tryTask {
                ApiResult.Success(runner())
            }
        }

        fun <T> tryTask(runner: () -> ApiResult<T>): ApiPromise<T> {
            val syncThread = SyncThreadService()
            val deferred = DeferredObject<ApiResult<T>, Exception, Any>()
            syncThread.execute {
                try {
                    deferred.resolve(runner())
                } catch (e: Exception) {
                    deferred.resolve(
                        ApiResult.Failure(
                            ApiError.Exception(e)
                        )
                    )
                }
            }
            return ApiPromise(deferred.promise())
        }

        /**
         * Run a block that itself calls async code which only returns via callbacks
         *
         * Example usage being okhttp async calls which execute on an internal thread pool
         *
         * @param[runner] the block to execute, will receive done and fail callbacks as params
         */
        fun <T> execute(runner: (done: (it: T) -> Unit, fail: (e: Exception) -> Unit) -> Unit): ApiPromise<T> {
            val deferred = DeferredObject<ApiResult<T>, Exception, Any>()
            try {
                runner({
                    deferred.resolve((ApiResult.Success(it)))
                }, {
                    deferred.resolve(ApiResult.Failure(ApiError.Exception(it)))
                })
            } catch (e: Exception) {
                deferred.resolve(ApiResult.Failure(ApiError.Exception(e)))
            }
            return ApiPromise(deferred.promise())
        }

        fun <T> of(item: T): ApiPromise<T> = of(ApiResult.Success(item))

        fun <T> of(error: ApiError): ApiPromise<T> = of(
            ApiResult.Failure(error)
        )

        fun <T> of(item: ApiResult<T>): ApiPromise<T> {
            val deferred = DeferredObject<ApiResult<T>, Exception, Any>()
            deferred.resolve(item)
            return ApiPromise(deferred.promise())
        }

        fun delay(millis: Long): ApiPromise<Unit> {
            return ApiPromise.task {
                Thread.sleep(millis)
            }
        }
    }

    infix fun <U> map(transform: (T) -> U): ApiPromise<U> =
        flatMap {
            of(
                ApiResult.Success(transform(it))
            )
        }

    infix fun <U> flatMap(transform: (T) -> ApiPromise<U>): ApiPromise<U> =
        flatMapResult {
            when (it) {
                is ApiResult.Success -> transform(it.value)
                is ApiResult.Failure -> of(
                    ApiResult.Failure(it.error)
                )
            }
        }

    infix fun mapError(transform: (ApiError) -> ApiError): ApiPromise<T> =
        mapResult {
            when (it) {
                is ApiResult.Success -> ApiResult.Success(
                    it.value
                )
                is ApiResult.Failure -> ApiResult.Failure<T>(
                    transform(it.error)
                )
            }
        }

    infix fun andThen(func: () -> Unit): ApiPromise<T> =
        mapResult {
            func()
            it
        }

    infix fun <U> andTry(transform: (T) -> ApiResult<U>): ApiPromise<U> =
        flatMap { of(transform(it)) }

    infix fun <U> mapResult(transform: (ApiResult<T>) -> ApiResult<U>): ApiPromise<U> =
        flatMapResult { of(transform(it)) }

    infix fun <U> flatMapResult(transform: (ApiResult<T>) -> ApiPromise<U>): ApiPromise<U> {
        val deferred = DeferredObject<ApiResult<U>, Exception, Any>()
        this.promise.done {
            try {
                transform(it).onComplete { deferred.resolve(it) }
            } catch (e: Exception) {
                deferred.resolve(
                    ApiResult.Failure(ApiError.Exception(e))
                )
            }
        }

        return ApiPromise(deferred.promise())
    }

    fun onComplete(callback: (ApiResult<T>) -> Unit) {
        this.promise.done {
            callback(it)
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    fun get(): ApiResult<T> {
        // JDeferred does not offer a way to block the current thread and get the inner value of
        // the promise. However, this is exactly what we want for tests. This is a hacky way of
        // providing such functionality.
        this.promise.waitSafely()
        var result: ApiResult<T>? = null
        this.promise.done { result = it }
        return result!!
    }

    @Throws(InterruptedException::class)
    fun waitForPromise() {
        this.promise.waitSafely()
    }
}
