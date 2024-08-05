package com.microsoft.notes.utils.network

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class NetworkAvailableObservableTest {
    private lateinit var networkSimulator: NetworkSimulator
    private lateinit var mockNetworkListener: (callback: () -> Unit) -> Unit

    @Before
    fun setup() {
        mockNetworkListener = {
            networkSimulator = NetworkSimulator(it)
        }
    }

    @Mock
    private val mockFunc = mock<() -> Unit>()

    @Test
    fun should_call_observers_as_soon_as_network_is_available() {
        val isNetworkConnected = { false }
        val observable = NetworkAvailableObservable(mockNetworkListener, isNetworkConnected)

        val observer = NetworkAvailableObserver(mockFunc, true)
        observable.addObserver(observer)
        verify(mockFunc, times(0))()

        networkSimulator.simulateNetworkGoingOnline()
        verify(mockFunc, times(1))()

        networkSimulator.simulateNetworkGoingOnline()
        verify(mockFunc, times(1))()
    }

    @Test
    fun should_call_observers_immediately_if_network_already_available() {
        val isNetworkConnected = { true }
        val observable = NetworkAvailableObservable(mockNetworkListener, isNetworkConnected)

        val observer = NetworkAvailableObserver(mockFunc, true)
        observable.addObserver(observer)
        verify(mockFunc, times(1))()

        networkSimulator.simulateNetworkGoingOnline()
        verify(mockFunc, times(1))()
    }

    @Test
    fun should_call_observers_multiple_times_if_specified() {
        val isNetworkConnected = { false }
        val observable = NetworkAvailableObservable(mockNetworkListener, isNetworkConnected)

        val observer = NetworkAvailableObserver(mockFunc, false)
        observable.addObserver(observer)
        verify(mockFunc, times(0))()

        networkSimulator.simulateNetworkGoingOnline()
        verify(mockFunc, times(1))()

        networkSimulator.simulateNetworkGoingOnline()
        verify(mockFunc, times(2))()
    }

    @Test
    fun should_call_observers_multiple_times_if_specified_and_network_already_online() {
        val isNetworkConnected = { true }
        val observable = NetworkAvailableObservable(mockNetworkListener, isNetworkConnected)

        val observer = NetworkAvailableObserver(mockFunc, false)
        observable.addObserver(observer)
        verify(mockFunc, times(1))()

        networkSimulator.simulateNetworkGoingOnline()
        verify(mockFunc, times(2))()

        networkSimulator.simulateNetworkGoingOnline()
        verify(mockFunc, times(3))()
    }

    @Test
    fun should_not_call_observers_if_removed() {
        val isNetworkConnected = { false }
        val observable = NetworkAvailableObservable(mockNetworkListener, isNetworkConnected)

        val observer = NetworkAvailableObserver(mockFunc, false)
        observable.addObserver(observer)
        verify(mockFunc, times(0))()

        observable.removeObserver(observer)
        networkSimulator.simulateNetworkGoingOnline()
        verify(mockFunc, times(0))()
    }
}

private class NetworkSimulator(private val callback: () -> Unit) {
    fun simulateNetworkGoingOnline() {
        callback()
    }
}
