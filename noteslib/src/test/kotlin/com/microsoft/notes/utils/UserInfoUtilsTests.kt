package com.microsoft.notes.utils

import android.content.Context
import android.content.SharedPreferences
import com.microsoft.notes.utils.logging.TestConstants
import com.microsoft.notes.utils.logging.TestConstants.Companion.TEST_USER_INFO_SUFFIX
import com.microsoft.notes.utils.utils.Constants
import com.microsoft.notes.utils.utils.UserInfoUtils
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.hamcrest.CoreMatchers.`is` as iz
import org.mockito.Mockito.`when` as testWhen

class UserInfoUtilsTests {
    @Mock val mockContext = mock<Context> {}
    @Mock val mockSharedPreferences = mock<SharedPreferences> {}
    @Mock val mockSharedPreferencesEditor = mock<SharedPreferences.Editor> {}
    private val userInfoSuffixSharedPrefsKeyForTestUser = UserInfoUtils.USER_INFO_SUFFIX + "_" + TestConstants.TEST_USER_ID

    @Before
    fun setup() {
        testWhen(mockContext.getSharedPreferences(Constants.AUTH_SHARED_PREF_KEY, Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        testWhen(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)
        testWhen(mockSharedPreferencesEditor.putString(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(mockSharedPreferencesEditor)
    }

    @Test
    fun should_provide_empty_user_info_suffix_for_empty_user_id() {
        val userInfoSuffix: String = UserInfoUtils.getUserInfoSuffix(Constants.EMPTY_USER_ID, mockContext)
        assertThat(userInfoSuffix, iz(""))
    }

    @Test
    fun should_provide_user_info_suffix_from_shared_prefs() {
        testWhen(
            mockSharedPreferences.getString(
                userInfoSuffixSharedPrefsKeyForTestUser,
                UserInfoUtils.DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE
            )
        )
            .thenReturn(TEST_USER_INFO_SUFFIX)

        val userInfoSuffix: String = UserInfoUtils.getUserInfoSuffix(TestConstants.TEST_USER_ID, mockContext)

        verify(mockSharedPreferences, times(0)).getBoolean(UserInfoUtils.FIRST_USER_INFO_SUFFIX_SET_SHARED_PREFS_KEY, false)
        assertThat(userInfoSuffix, iz(TEST_USER_INFO_SUFFIX))
    }

    @Test
    fun should_provide_empty_user_info_suffix_for_first_userID() {
        testWhen(mockSharedPreferences.getBoolean(UserInfoUtils.FIRST_USER_INFO_SUFFIX_SET_SHARED_PREFS_KEY, false)).thenReturn(false)
        testWhen(
            mockSharedPreferences.getString(
                userInfoSuffixSharedPrefsKeyForTestUser,
                UserInfoUtils
                    .DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE
            )
        ).thenReturn(UserInfoUtils.DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE)

        val userInfoSuffix: String = UserInfoUtils.getUserInfoSuffix(TestConstants.TEST_USER_ID, mockContext)
        assertThat(userInfoSuffix, iz(""))
    }

    @Test
    fun should_provide_user_info_suffix_from_shared_pref() {
        testWhen(
            mockSharedPreferences.getString(
                userInfoSuffixSharedPrefsKeyForTestUser,
                UserInfoUtils.DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE
            )
        )
            .thenReturn(TEST_USER_INFO_SUFFIX)
        testWhen(mockSharedPreferences.getBoolean(UserInfoUtils.FIRST_USER_INFO_SUFFIX_SET_SHARED_PREFS_KEY, false)).thenReturn(true)

        val userInfoSuffix: String = UserInfoUtils.getUserInfoSuffix(TestConstants.TEST_USER_ID, mockContext)

        assertThat(userInfoSuffix, iz(TEST_USER_INFO_SUFFIX))
    }

    @Test
    fun should_provide_non_empty_user_info_suffix_for_unset_userID_in_shared_prefs() {
        testWhen(
            mockSharedPreferences.getString(
                userInfoSuffixSharedPrefsKeyForTestUser,
                UserInfoUtils.DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE
            )
        )
            .thenReturn(UserInfoUtils.DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE)
        testWhen(mockSharedPreferences.getBoolean(UserInfoUtils.FIRST_USER_INFO_SUFFIX_SET_SHARED_PREFS_KEY, false)).thenReturn(true)
        val userInfoSuffix: String = UserInfoUtils.getUserInfoSuffix(TestConstants.TEST_USER_ID, mockContext)
        assert(userInfoSuffix.isNotEmpty())
    }

    // Signout related tests
    @Test
    fun should_non_reset_user_info_suffix_for_absent_values_on_signout() {
        testWhen(
            mockSharedPreferences.getString(
                userInfoSuffixSharedPrefsKeyForTestUser,
                UserInfoUtils.DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE
            )
        )
            .thenReturn(UserInfoUtils.DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE)

        UserInfoUtils.updateUserInfoSuffixForSignedOutUser(TestConstants.TEST_USER_ID, mockContext)

        verify(mockSharedPreferencesEditor, times(0)).remove(userInfoSuffixSharedPrefsKeyForTestUser)
        verify(mockSharedPreferencesEditor, times(0)).putBoolean(
            UserInfoUtils
                .FIRST_USER_INFO_SUFFIX_SET_SHARED_PREFS_KEY,
            false
        )
    }

    @Test
    fun should_reset_user_info_suffix_for_random_values_on_signout() {
        testWhen(
            mockSharedPreferences.getString(
                userInfoSuffixSharedPrefsKeyForTestUser,
                UserInfoUtils.DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE
            )
        )
            .thenReturn(TEST_USER_INFO_SUFFIX)

        testWhen(mockSharedPreferencesEditor.remove(userInfoSuffixSharedPrefsKeyForTestUser)).thenReturn(mockSharedPreferencesEditor)

        UserInfoUtils.updateUserInfoSuffixForSignedOutUser(TestConstants.TEST_USER_ID, mockContext)

        verify(mockSharedPreferencesEditor, times(1)).remove(userInfoSuffixSharedPrefsKeyForTestUser)
        verify(mockSharedPreferencesEditor, times(0)).putBoolean(
            UserInfoUtils
                .FIRST_USER_INFO_SUFFIX_SET_SHARED_PREFS_KEY,
            false
        )
    }

    @Test
    fun should_reset_user_info_suffix_for_first_user_on_signout() {
        testWhen(
            mockSharedPreferences.getString(
                userInfoSuffixSharedPrefsKeyForTestUser,
                UserInfoUtils.DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE
            )
        )
            .thenReturn("")

        testWhen(mockSharedPreferencesEditor.remove(userInfoSuffixSharedPrefsKeyForTestUser)).thenReturn(mockSharedPreferencesEditor)
        testWhen(
            mockSharedPreferencesEditor.putBoolean(
                UserInfoUtils
                    .FIRST_USER_INFO_SUFFIX_SET_SHARED_PREFS_KEY,
                false
            )
        ).thenReturn(mockSharedPreferencesEditor)

        UserInfoUtils.updateUserInfoSuffixForSignedOutUser(TestConstants.TEST_USER_ID, mockContext)

        verify(mockSharedPreferencesEditor, times(1)).remove(userInfoSuffixSharedPrefsKeyForTestUser)
        verify(mockSharedPreferencesEditor, times(1)).putBoolean(
            UserInfoUtils
                .FIRST_USER_INFO_SUFFIX_SET_SHARED_PREFS_KEY,
            false
        )
    }
}
