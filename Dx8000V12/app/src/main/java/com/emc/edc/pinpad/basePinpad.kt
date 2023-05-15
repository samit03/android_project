package com.emc.edc.pinpad

import com.usdk.apiservice.aidl.BaseError
import com.usdk.apiservice.aidl.pinpad.PinpadError

open class BasePinpad {

    fun getErrorMessage(error: Int): String? {
        val message: String
        message = when (error) {

            PinpadError.ERROR_ABOLISH -> "ERROR_ABOLISH"
            PinpadError.ERROR_ACCESSING_KAP_DENY -> "ERROR_ACCESSING_KAP_DENY"
            PinpadError.ERROR_BAD_KEY_USAGE -> "ERROR_BAD_KEY_USAGE"
            PinpadError.ERROR_BAD_MODE_OF_KEY_USE -> "ERROR_BAD_MODE_OF_KEY_USE"
            PinpadError.ERROR_BAD_STATUS -> "ERROR_BAD_STATUS"
            PinpadError.ERROR_BUSY -> "ERROR_BUSY"
            PinpadError.ERROR_CANCELLED_BY_USER -> "ERROR_CANCELLED_BY_USER"
            PinpadError.ERROR_COMM_ERR -> "ERROR_COMM_ERR"
            PinpadError.ERROR_DUKPT_COUNTER_OVERFLOW -> "ERROR_DUKPT_COUNTER_OVERFLOW"
            PinpadError.ERROR_DUKPT_NOT_INITED -> "ERROR_DUKPT_NOT_INITED"
            PinpadError.ERROR_ENC_KEY_FMT_TOO_SIMPLE -> "ERROR_ENC_KEY_FMT_TOO_SIMPLE"
            PinpadError.ERROR_ENCRYPT_MAG_TRACK_TOO_FREQUENTLY -> "ERROR_ENCRYPT_MAG_TRACK_TOO_FREQUENTLY"
            PinpadError.ERROR_OTHERERR -> "ERROR_OTHERERR"
            PinpadError.ERROR_FAIL_TO_AUTH -> "ERROR_FAIL_TO_AUTH"
            PinpadError.ERROR_INCOMPATIBLE_KEY_SYSTEM -> "ERROR_INCOMPATIBLE_KEY_SYSTEM"
            PinpadError.ERROR_INVALID_ARGUMENT -> "ERROR_INVALID_ARGUMENT"
            PinpadError.ERROR_INVALID_KEY_HANDLE -> "ERROR_INVALID_KEY_HANDLE"
            PinpadError.ERROR_KAP_ALREADY_EXIST -> "ERROR_KAP_ALREADY_EXIST"
            PinpadError.ERROR_ARGUMENT_CONFLICT -> "ERROR_ARGUMENT_CONFLICT"
            PinpadError.ERROR_KEYBUNDLE_ERR -> "ERROR_KEYBUNDLE_ERR"
            PinpadError.ERROR_NO_ENOUGH_SPACE -> "ERROR_NO_ENOUGH_SPACE"
            PinpadError.ERROR_NO_PIN_ENTERED -> "ERROR_NO_PIN_ENTERED"
            PinpadError.ERROR_NO_SUCH_KAP -> "ERROR_NO_SUCH_KAP"
            PinpadError.ERROR_NO_SUCH_KEY -> "ERROR_NO_SUCH_KEY"
            PinpadError.ERROR_NO_SUCH_PINPAD -> "ERROR_NO_SUCH_PINPAD"
            PinpadError.ERROR_PERMISSION_DENY -> "ERROR_PERMISSION_DENY"
            PinpadError.ERROR_PIN_ENTRY_TOO_FREQUENTLY -> "ERROR_PIN_ENTRY_TOO_FREQUENTLY"
            PinpadError.ERROR_REFER_TO_KEY_OUTSIDE_KAP -> "ERROR_REFER_TO_KEY_OUTSIDE_KAP"
            PinpadError.ERROR_REOPEN_PINPAD -> "ERROR_REOPEN_PINPAD"
            PinpadError.ERROR_SAME_KEY_VALUE_DETECTED -> "ERROR_SAME_KEY_VALUE_DETECTED"
            PinpadError.ERROR_SERVICE_DIED -> "ERROR_SERVICE_DIED"
            PinpadError.ERROR_TIMEOUT -> "ERROR_TIMEOUT"
            PinpadError.ERROR_UNSUPPORTED_FUNC -> "ERROR_UNSUPPORTED_FUNC"
            PinpadError.ERROR_WRONG_KAP_MODE -> "ERROR_WRONG_KAP_MODE"
            PinpadError.ERROR_KCV -> "ERROR_KCV"
            PinpadError.ERROR_INPUT_TIMEOUT -> "ERROR_INPUT_TIMEOUT"
            PinpadError.ERROR_INPUT_COMM_ERR -> "ERROR_INPUT_COMM_ERR"
            PinpadError.ERROR_INPUT_UNKNOWN -> "ERROR_INPUT_UNKNOWN"
            PinpadError.ERROR_NOT_CERT -> "ERROR_NOT_CERT"
            else -> "Unknown error"
        }
        return message
    }
}