package com.emc.edc.emv.tag_finnal_select

import androidx.compose.runtime.MutableState
import com.emc.edc.database.ConfigAIDRO
import com.usdk.apiservice.aidl.emv.EMVTag

fun AmexTag(
    hasError: MutableState<Boolean>? = null,
    errorMessage: MutableState<String>? = null,
    finaldata: ConfigAIDRO
): String {
    val tagAidData = finaldata
    var tlvList = StringBuilder()
    try {

        if (tagAidData.terminal_capability != "") {
            tlvList.append(
                EMVTag.EMV_TAG_TM_CAP + (tagAidData.terminal_capability!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.terminal_capability
            )
        }
        if (tagAidData.add_terminal_capability != "") {

            tlvList.append(
                EMVTag.EMV_TAG_TM_CAP_AD + (tagAidData.add_terminal_capability!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.add_terminal_capability
            )
        }
        if (tagAidData.tac_denial != "") {
            tlvList.append(
                EMVTag.DEF_TAG_TAC_DECLINE + (tagAidData.tac_denial!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.tac_denial
            )
        }
        if (tagAidData.tac_online != "") {
            tlvList.append(
                EMVTag.DEF_TAG_TAC_ONLINE + (tagAidData.tac_online!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.tac_online
            )
        }

        if (tagAidData.tac_default != "") {
            tlvList.append(
                EMVTag.DEF_TAG_TAC_DEFAULT + (tagAidData.tac_default!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.tac_default
            )
        }
        if (tagAidData.contactless_transactionlimit != "") {
            tlvList.append(
                EMVTag.A_TAG_TM_TRANS_LIMIT + (tagAidData.contactless_transactionlimit!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.contactless_transactionlimit
            )
        }

        if (tagAidData.contactless_floorlimit != "") {
            tlvList.append(
                EMVTag.A_TAG_TM_FLOOR_LIMIT + (tagAidData.contactless_floorlimit!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.contactless_floorlimit
            )
        }
        if (tagAidData.contacless_cvmlimit != "") {
            tlvList.append(
                EMVTag.A_TAG_TM_CVM_LIMIT + (tagAidData.contacless_cvmlimit!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.contacless_cvmlimit
            )
        }

    }
    catch (e:Exception)
    {
        hasError!!.value = true
        errorMessage!!.value = e.message.toString()
        return ""
    }
    return tlvList.toString()
}