package com.microsoft.notes.utils.utils

import android.content.Context
import com.microsoft.notes.models.AccountType
import java.lang.IllegalArgumentException

fun IdentityMetaData.toUserInfo(context: Context): UserInfo {
    val routingPrefix = accountType.getDefaultRoutingPrefix()
    return UserInfo(
        userID = userID,
        routingPrefix = routingPrefix,
        email = email,
        accessToken = accessToken,
        accountType = accountType,
        tenantID = getTenantId(accountType, routingPrefix, null),
        userInfoSuffix = UserInfoUtils.getUserInfoSuffix(userID, context = context)
    )
}

fun PrefixedIdentityMetaData.toUserInfo(context: Context): UserInfo {
    throwIfAccountTypeInvalid()
    val routingPrefix = userIDType.toRoutingPrefix()
    return UserInfo(
        userID = userID,
        routingPrefix = userIDType.toRoutingPrefix(),
        email = email,
        accessToken = accessToken,
        accountType = accountType,
        tenantID = getTenantId(accountType, routingPrefix, tenantIDForADAL),
        userInfoSuffix = UserInfoUtils.getUserInfoSuffix(userID, context = context)
    )
}

private fun PrefixedIdentityMetaData.throwIfAccountTypeInvalid() {
    if (accountType == AccountType.ADAL && userIDType == UserIDType.CID) {
        throw IllegalArgumentException("CID is not a valid UserIDType for ADAL")
    }
    if (accountType == AccountType.MSA && userIDType == UserIDType.OID) {
        throw IllegalArgumentException("OID is not a valid UserIDType for MSA")
    }
}

private fun AccountType.getDefaultRoutingPrefix(): RoutingPrefix {
    return if (this == AccountType.MSA) {
        RoutingPrefix.CID
    } else {
        RoutingPrefix.Unprefixed
    }
}

private fun UserIDType.toRoutingPrefix(): RoutingPrefix {
    return when (this) {
        UserIDType.OID -> RoutingPrefix.OID
        UserIDType.PUID -> RoutingPrefix.PUID
        UserIDType.CID -> RoutingPrefix.CID
    }
}

private fun getTenantId(accountType: AccountType, routingPrefix: RoutingPrefix, tenantIDForADAL: String?): String {
    return when (accountType) {
        AccountType.ADAL -> {
            if ((routingPrefix == RoutingPrefix.OID || routingPrefix == RoutingPrefix.PUID) &&
                tenantIDForADAL.isNullOrBlank()
            ) {
                throw IllegalArgumentException(
                    "If the account is ADAL and using a routing prefix that requires " +
                        "tenantID, tenantIDForADAL should be a valid tenant guid"
                )
            }
            tenantIDForADAL ?: Constants.EMPTY_TENANT_ID
        }
        AccountType.MSA -> "84df9e7f-e9f6-40af-b435-aaaaaaaaaaaa"
        AccountType.UNDEFINED -> ""
    }
}
