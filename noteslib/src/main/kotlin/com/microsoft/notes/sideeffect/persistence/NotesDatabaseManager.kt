package com.microsoft.notes.sideeffect.persistence

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.microsoft.notes.models.AccountType
import com.microsoft.notes.sideeffect.persistence.extensions.deleteAll
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.utils.Constants
import com.microsoft.notes.utils.utils.RoutingPrefix
import com.microsoft.notes.utils.utils.UserInfo
import com.microsoft.notes.utils.utils.UserInfoUtils

// ToDo:- Add Logic to cleanup databases whose userIds are lost
// https://github.com/microsoft-notes/notes-android-sdk/issues/880
class NotesDatabaseManager(
    val context: Context,
    val dbName: String,
    val multiAccountEnabled: Boolean,
    val notesLogger: NotesLogger?
) {
    private val notesDBMap = mutableMapOf<String, NotesDatabase>()

    init {
        if (multiAccountEnabled) {
            val signedInEmails = context
                .getSharedPreferences(Constants.AUTH_SHARED_PREF_KEY, MODE_PRIVATE).getStringSet(
                    Constants
                        .SIGNED_IN_ACCOUNTS,
                    emptySet()
                )
            if (signedInEmails?.isNotEmpty() == true) {
                signedInEmails.forEach {
                    val userInfoSuffix = UserInfoUtils.getUserInfoSuffix(it, context)
                    // AccountType doesn't matter here, we are interested in userID and userInfoSuffix only.
                    handleNewUser(
                        UserInfo(
                            userID = it, userInfoSuffix = userInfoSuffix,
                            accountType = AccountType
                                .UNDEFINED,
                            accessToken = Constants.EMPTY_ACCESS_TOKEN, email = Constants.EMPTY_EMAIL,
                            routingPrefix = RoutingPrefix.Unprefixed, tenantID = Constants.EMPTY_TENANT_ID
                        )
                    )
                }
            } else {
                handleNewUser(UserInfo.EMPTY_USER_INFO)
            }
        } else {
            handleNewUser(UserInfo.EMPTY_USER_INFO)
        }
    }

    companion object {
        private const val LOG_TAG = "NotesDatabaseManager"
    }

    fun getNotesDatabaseForUser(userID: String): NotesDatabase? =
        if (multiAccountEnabled)
            notesDBMap.get(userID)
        else
            notesDBMap.get(Constants.EMPTY_USER_ID)

    /**
     * This api handles a new user signin and returns
     * database name for intune protection
     */
    fun handleNewUser(userInfo: UserInfo): String {
        if (!multiAccountEnabled) {
            handleNewUserWithMultiAccountDisabled()
        } else {
            handleNewUserWithMultiAccountEnabled(userInfo)
        }

        return getDatabaseName(userInfo)
    }

    fun handleLogout(userID: String) {
        getNotesDatabaseForUser(userID)?.deleteAll()
    }

    private fun handleNewUserWithMultiAccountDisabled() {
        if (!notesDBMap.containsKey(Constants.EMPTY_USER_ID)) {
            val notesDB = createNewNotesDatabase(
                UserInfo.EMPTY_USER_INFO
            )
            notesDBMap.put(Constants.EMPTY_USER_ID, notesDB)
        }
    }

    private fun handleNewUserWithMultiAccountEnabled(userInfo: UserInfo) {
        if (userInfo.userID == Constants.EMPTY_USER_ID) {
            val notesDB = createNewNotesDatabase(userInfo)
            notesDBMap[Constants.EMPTY_USER_ID] = notesDB
        } else {
            if (!notesDBMap.containsKey(userInfo.userID)) {
                val notesDB = notesDBMap[Constants.EMPTY_USER_ID]
                if (notesDB != null) {
                    notesDBMap[userInfo.userID] = notesDB
                    notesDBMap.remove(Constants.EMPTY_USER_ID)
                } else {
                    notesDBMap[userInfo.userID] = createNewNotesDatabase(userInfo)
                }
            }
        }
    }

    private fun createNewNotesDatabase(userInfo: UserInfo): NotesDatabase =
        createNotesDB(context = context, dbName = getDatabaseName(userInfo))

    private fun getDatabaseName(userInfo: UserInfo): String =
        if (userInfo.userInfoSuffix.isNotEmpty())
            dbName + "_" + userInfo.userInfoSuffix
        else
            dbName
}
