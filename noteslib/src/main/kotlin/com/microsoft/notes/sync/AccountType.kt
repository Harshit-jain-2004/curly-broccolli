package com.microsoft.notes.sync

// This enum should be in sync with AccountType in Models.kt except for UNDEFINED
enum class AccountType(val value: Int) {
    UNDEFINED(-1),
    MSA(0),
    ADAL(1);

    companion object {
        private val accountMap = AccountType.values().associateBy(
            AccountType::value
        )
        fun fromInt(accountType: Int): AccountType = accountMap[accountType] ?: UNDEFINED
    }
}
