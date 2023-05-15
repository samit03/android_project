package com.emc.edc.emv.tag_finnal_select

import androidx.compose.runtime.MutableState
import com.emc.edc.database.ConfigAIDRO
import com.usdk.apiservice.aidl.emv.EMVTag
import org.json.JSONObject

fun EmvTag(
    jsonData: MutableState<JSONObject>? = null,
    hasError: MutableState<Boolean>? = null,
    errorMessage: MutableState<String>? = null,
    finaldata: ConfigAIDRO
): String {
    val tagAidData = finaldata
    var tlvList = StringBuilder()
    try {
        if (tagAidData.aid_version != "") {
            tlvList
                .append(
                    EMVTag.EMV_TAG_TM_APPVERNO + (tagAidData.aid_version!!.length / 2).toString()
                        .padStart(2, '0') + tagAidData.aid_version
                )
        }
        if (tagAidData.add_terminal_capability != "") {
            tlvList
                .append(
                    EMVTag.EMV_TAG_TM_CAP_AD + (tagAidData.add_terminal_capability!!.length / 2).toString()
                        .padStart(2, '0') + tagAidData.add_terminal_capability
                )
        }

        if (tagAidData.terminal_capability != "") {
            tlvList
                .append(
                    EMVTag.EMV_TAG_TM_CAP + (tagAidData.terminal_capability!!.length / 2).toString()
                        .padStart(2, '0') + tagAidData.terminal_capability
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
        // tlvList.append(EMVTag.DEF_TAG_GAC_CONTROL + "01" + tagAidData.gac_control)
        if (tagAidData.target_percent != "") {
            tlvList.append(
                EMVTag.DEF_TAG_RAND_SLT_PER + (tagAidData.target_percent!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.target_percent
            )// tagAidData.target_percent)
        }
        if (tagAidData.max_target_percent != "") {
            tlvList.append(
                EMVTag.DEF_TAG_RAND_SLT_MAXPER + (tagAidData.max_target_percent!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.max_target_percent
            )
        }
        if (tagAidData.threshold != "") {
            tlvList.append(
                EMVTag.DEF_TAG_RAND_SLT_THRESHOLD + (tagAidData.threshold!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.threshold
            )//tagAidData!!.threshold)
        }

        if (tagAidData.floor_limit != "") {
            tlvList.append(
                EMVTag.EMV_TAG_TM_FLOORLMT + (tagAidData!!.floor_limit!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData!!.floor_limit
            )

        }
//        if (jsonData!!.value.optString("transaction_type") == "offline_sale") {
//            tlvList.append(
//                EMVTag.DEF_TAG_GAC_CONTROL + "01" + "01"
//            )
//        }
    } catch (e: Exception) {
        hasError!!.value = true
        errorMessage!!.value = e.message.toString()
        return ""
    }
    return tlvList.toString()
}