package com.emc.edc.emv.tag_finnal_select

import androidx.compose.runtime.MutableState
import com.emc.edc.configDataRealmConfiguration
import com.emc.edc.database.ConfigAIDRO
import com.emc.edc.database.ConfigTransactionRO
import com.emc.edc.getTransactionSequentCounter
import com.usdk.apiservice.aidl.emv.EMVTag
import io.realm.Realm
import io.realm.kotlin.where
import org.json.JSONObject
import java.util.*

fun ConmonTag(
    hasError: MutableState<Boolean>? = null,
    errorMessage: MutableState<String>? = null,
    jsonData: MutableState<JSONObject>? = null,
    finaldata: ConfigAIDRO
): String {
    var tlvList = StringBuilder()
    try {

        val tagAidData = finaldata
        val realm = Realm.getInstance(configDataRealmConfiguration)
        val transactionType = realm.where<ConfigTransactionRO>()
            .equalTo("type", jsonData!!.value.getString("transaction_type")).findFirst()
        val processingCode = transactionType!!.processing_code!![0]
        var amount =
            jsonData!!.value.getString("amount").replace(".", "")
                .replace(",", "").padStart(12, '0')

        val currentDate = "${Date().year - 100}" +
                "${if (1 + Date().month < 10) "0${1 + Date().month}" else 1 + Date().month}" +
                "${if (Date().date < 10) "0${Date().date}" else Date().date}"

        val currentTime =
            Date().hours.toString().padStart(2, '0') + Date().minutes.toString()
                .padStart(2, '0') + Date().seconds.toString()
                .padStart(2, '0')

        if (amount != "") {
            tlvList
                .append(
                    EMVTag.EMV_TAG_TM_AUTHAMNTN + (amount.length / 2).toString()
                        .padStart(2, '0') + amount
                )
        } else {
            tlvList
                .append(
                    EMVTag.EMV_TAG_TM_AUTHAMNTN + "0" // need to edit
                        .padStart(2, '0') + amount
                )
        }
            .append(
                EMVTag.EMV_TAG_TM_TRANSDATE + (currentDate.length / 2).toString()
                    .padStart(2, '0') + currentDate
            )
            .append(
                EMVTag.EMV_TAG_TM_TRANSTIME + (currentTime.length / 2).toString()
                    .padStart(2, '0') + currentTime
            )

        tlvList.append(
            EMVTag.EMV_TAG_TM_CNTRYCODE + (tagAidData!!.terminal_country_code!!.length / 2).toString()
                .padStart(2, '0') + tagAidData!!.terminal_country_code
        )
        tlvList.append(
            EMVTag.EMV_TAG_TM_CURCODE + (tagAidData!!.terminal_currency_code!!.length / 2).toString()
                .padStart(2, '0') + tagAidData.terminal_currency_code
        )
        tlvList.append(
            EMVTag.EMV_TAG_TM_TRANSTYPE + (processingCode.toString().length / 2).toString()
                .padStart(2, '0') + processingCode.toString()
        )
        tlvList.append(
            EMVTag.EMV_TAG_TM_TERMTYPE + (tagAidData!!.terminal_type!!.length / 2).toString()
                .padStart(2, '0') + tagAidData.terminal_type
        )
        tlvList.append(EMVTag.EMV_TAG_TM_TRSEQCNTR + "04" + getTransactionSequentCounter())
            .append("9F0306000000000000") // Other amount
            .append("9F1E084344383233383436") // serial number of the terminal


    } catch (e: Exception) {
        hasError!!.value = true
        errorMessage!!.value = e.message.toString()
        return ""
    }
    return tlvList.toString()
}


