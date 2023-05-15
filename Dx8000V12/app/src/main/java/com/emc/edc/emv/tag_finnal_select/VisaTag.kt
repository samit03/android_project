package com.emc.edc.emv.tag_finnal_select

import androidx.compose.runtime.MutableState
import com.emc.edc.database.ConfigAIDRO
import com.usdk.apiservice.aidl.emv.EMVTag

fun VisaTag(
    hasError: MutableState<Boolean>? = null,
    errorMessage: MutableState<String>? = null,
    finaldata: ConfigAIDRO
): String {
    val tagAidData = finaldata
    var tlvList = StringBuilder()
    try {

        tlvList.append(
            EMVTag.EFTPOS_TAG_TM_TTQ + "04" + "36004000"
        )

        tlvList.append(
            EMVTag.C_TAG_TM_RD_RCP + "02" + "FF00"
        )
        if (tagAidData.contactless_transactionlimit != "") {
            tlvList.append(
                EMVTag.V_TAG_TM_TRANS_LIMIT + (tagAidData.contactless_transactionlimit!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.contactless_transactionlimit
            )
        }
        if (tagAidData.contactless_floorlimit != "") {
            tlvList.append(
                EMVTag.V_TAG_TM_FLOOR_LIMIT + (tagAidData.contactless_floorlimit!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.contactless_floorlimit
            )
        }
        if (tagAidData.contacless_cvmlimit != "") {
            tlvList.append(
                EMVTag.V_TAG_TM_CVM_LIMIT + (tagAidData.contacless_cvmlimit!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.contacless_cvmlimit
            )

        }
        if (tagAidData.floor_limit != "") {
            tlvList.append(
                EMVTag.EMV_TAG_TM_FLOORLMT + (tagAidData!!.floor_limit!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData!!.floor_limit
            )

        }


    } catch (e: Exception) {
        hasError!!.value = true
        errorMessage!!.value = e.message.toString()
        return ""
    }
    return tlvList.toString()
}