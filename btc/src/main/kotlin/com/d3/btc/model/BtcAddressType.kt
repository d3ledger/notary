package com.d3.btc.model

import com.d3.commons.util.getRandomId

//Enumeration full of Bitcoin address types
enum class BtcAddressType(val title: String) {
    FREE("free"), CHANGE("change");

    //Creates session account name
    fun createSessionAccountName() =
        "${this.title}_${String.getRandomId()}".substring(0, 32)
}

//Returns Bitcoin address type using account id
fun getAddressTypeByAccountId(accountId: String) =
    BtcAddressType.values().find { addressType -> accountId.startsWith(addressType.title) }!!
