package com.d3.commons.util

import com.d3.commons.sidechain.iroha.consumer.status.ToriiErrorResponseException

private const val BAD_OLD_VALUE_ERROR_CODE = 4
private const val COMPARE_AND_SET_DETAIL_COMMAND = "compareAndSetAccountDetail"

/**
 * Checks if a given exception is related to CAS issues
 * @param ex - exception to check
 * @return true if it's related to CAS issues
 */
fun isCASError(ex: Exception) =
    ex is ToriiErrorResponseException &&
            ex.toriiResponse.errorCode == BAD_OLD_VALUE_ERROR_CODE &&
            ex.toriiResponse.errOrCmdName == COMPARE_AND_SET_DETAIL_COMMAND

