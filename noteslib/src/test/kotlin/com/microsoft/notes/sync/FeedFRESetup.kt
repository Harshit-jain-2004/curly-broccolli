package com.microsoft.notes.sync

import android.content.Context
import android.content.SharedPreferences
import com.microsoft.notes.utils.logging.TestConstants
import com.microsoft.notes.utils.utils.Constants
import org.junit.Before
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.mock

open class FeedFRESetup {
    @Mock
    val mockContext = mock<Context> {}
    @Mock
    val mockSharedPreferences = mock<SharedPreferences> {}
    @Mock
    val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> {}

    @Before
    fun setup() {
        Mockito.`when`(mockContext.getSharedPreferences(Constants.FEED_FRE_SYNC_PREFERENCE, Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        Mockito.`when`(mockSharedPreferences.getString(Constants.FEED_FRE_SYNC_PREFERENCE_KEY + "_" + TestConstants.TEST_USER_ID, null)).thenReturn("FRESyncCompleted")
        Mockito.`when`(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)
        Mockito.`when`(mockSharedPreferencesEditor.putString(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(mockSharedPreferencesEditor)
    }
}
