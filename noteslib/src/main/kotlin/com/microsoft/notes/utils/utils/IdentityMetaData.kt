package com.microsoft.notes.utils.utils

import com.microsoft.notes.models.AccountType
import java.io.Serializable

data class IdentityMetaData(
    val userID: String,
    val email: String,
    val accessToken: String,
    val accountType: AccountType
) : Serializable

data class PrefixedIdentityMetaData(
    val userID: String,
    val userIDType: UserIDType,
    val email: String,
    val accessToken: String,
    val accountType: AccountType,
    val tenantIDForADAL: String?
) : Serializable

enum class UserIDType {
    OID,
    PUID,
    CID
}
