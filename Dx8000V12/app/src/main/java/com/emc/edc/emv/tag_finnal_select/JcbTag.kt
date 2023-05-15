package com.emc.edc.emv.tag_finnal_select

import androidx.compose.runtime.MutableState
import com.emc.edc.database.ConfigAIDRO
import com.usdk.apiservice.aidl.emv.EMVTag

fun JcbTag(
    hasError: MutableState<Boolean>? = null,
    errorMessage: MutableState<String>? = null,
    finaldata: ConfigAIDRO
): String {
    val tagAidData = finaldata
    var tlvList = StringBuilder()
    try {

        tlvList.append(
            EMVTag.EMV_TAG_TM_CUREXP + "01" + "02" //5F36
        )


        tlvList.append(
            EMVTag.DEF_TAG_J_COMB_OPTION + "02" + "7B00" //DF918404
        )

        tlvList.append(
            EMVTag.DEF_TAG_J_TIP + "03" + "708000" //DF918408
        )

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
                EMVTag.DEF_TAG_J_TRANS_LIMIT + (tagAidData.contactless_transactionlimit!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.contactless_transactionlimit
            )
        }
        if (tagAidData.contactless_floorlimit != "") {
            tlvList.append(
                EMVTag.DEF_TAG_J_FLOOR_LIMIT + (tagAidData.contactless_floorlimit!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.contactless_floorlimit
            )
        }
        if (tagAidData.max_target_percent != "") {
            tlvList.append(
                EMVTag.DEF_TAG_J_RS_MAX_PERCENT + (tagAidData.max_target_percent!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.max_target_percent
            )
        }
        if (tagAidData.target_percent != "") {
            tlvList.append(
                EMVTag.DEF_TAG_J_RS_TARGET_PERCENT + (tagAidData.target_percent!!.length / 2).toString()
                    .padStart(2, '0') + tagAidData.target_percent
            )// tagAidData.target_percent)
        }

//        if (tagAidData.threshold != "") {
//            val hexString = Integer.parseInt(tagAidData.threshold).toString(16).padStart(8,'0')
//            tlvList.append(
//                EMVTag.DEF_TAG_J_RS_THRESH_VALUE + (hexString.length / 2).toString()
//                    .padStart(2, '0') + hexString
//            )//tagAidData!!.threshold)
//        }

    } catch (e: Exception) {
        hasError!!.value = true
        errorMessage!!.value = e.message.toString()
        return ""
    }
    return tlvList.toString()
}