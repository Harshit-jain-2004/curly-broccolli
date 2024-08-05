package com.microsoft.notes.utils.network

class NetworkAvailableObservable(
    callWheneverNetworkIsAvailable: (callback: () -> Unit) -> Unit,
    private val isNetworkConnected: () -> Boolean
) {
    private val observers = mutableSetOf<NetworkAvailableObserver>()

    init {
        callWheneverNetworkIsAvailable {
            synchronized(observers) {
                val iterator = observers.iterator()
                while (iterator.hasNext()) {
                    val observer: NetworkAvailableObserver = iterator.next()
                    if (observer.removeWhenCalled) {
                        iterator.remove()
                    }
                    observer.func()
                }
            }
        }
    }

    fun addObserver(observer: NetworkAvailableObserver) {
        synchronized(observers) {
            observers.add(observer)
            if (isNetworkConnected()) {
                call(observer)
            }
        }
    }

    fun removeObserver(observer: NetworkAvailableObserver) {
        observers.remove(observer)
    }

    private fun call(observer: NetworkAvailableObserver) {
        if (observer.removeWhenCalled) {
            observers.remove(observer)
        }
        observer.func()
    }
}

data class NetworkAvailableObserver(
    val func: () -> Unit,
    val removeWhenCalled: Boolean
)
