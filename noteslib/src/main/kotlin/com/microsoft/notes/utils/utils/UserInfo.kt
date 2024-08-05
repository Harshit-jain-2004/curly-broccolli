package com.microsoft.notes.utils.utils

import com.microsoft.notes.models.AccountType

data class UserInfo(
    val userID: String,
    val routingPrefix: RoutingPrefix,
    val email: String,
    val accessToken: String,
    val accountType: AccountType,
    val tenantID: String,
    val userInfoSuffix: String
) {
    companion object {
        val EMPTY_USER_INFO = UserInfo(
            userID = Constants.EMPTY_USER_ID,
            routingPrefix = RoutingPrefix.Unprefixed,
            email = Constants.EMPTY_EMAIL,
            accessToken = Constants.EMPTY_ACCESS_TOKEN,
            accountType = AccountType.UNDEFINED,
            tenantID = Constants.EMPTY_TENANT_ID,
            userInfoSuffix = Constants.EMPTY_USER_INFO_SUFFIX
        )
    }
}

enum class RoutingPrefix {
    OID,
    PUID,
    CID,
    Unprefixed
}

fun UserInfo.getRoutingKey(): String {
    return when (routingPrefix) {
        RoutingPrefix.OID -> "Oid:$userID@$tenantID"
        RoutingPrefix.PUID -> "PUID:$userID@$tenantID"
        RoutingPrefix.CID -> "CID:$userID"
        RoutingPrefix.Unprefixed -> userID
    }
}
