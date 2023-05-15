package com.emc.edc.screen.utils

import android.util.Log
import androidx.compose.runtime.MutableState
import com.emc.edc.getCardControl
import com.emc.edc.getTraceInvoice
import com.emc.edc.selectCardData
import com.emc.edc.selectHost
import org.json.JSONObject


fun getHostInformationFromCard(
    jsonData: MutableState<JSONObject>,
    hasError: MutableState<Boolean>,
    errorMessage: MutableState<String>,
    ): Boolean {
    //val hasError = mutableStateOf(false)
    //val description = mutableStateOf("")
    var cardData = selectCardData(jsonData.value.getString("card_number"))
    var returnData = false
    //cardData = null
    if (cardData != null) {
        var host = selectHost(cardData!!.host_record_index!!)
        val transactionType = jsonData.value.getString("transaction_type")
        val cardControl =
            getCardControl(cardData.card_control_record_index!!, transactionType!!)
        Log.d("test", "card data: $cardData")
        Log.d("test", "card control: $cardControl")
        if (cardControl != null && jsonData != null) {
            if (cardControl.checkAllow(transactionType)!!) {
                jsonData.value.put("card_record_index", cardData.card_record_index!!)
                jsonData.value.put("card_label", cardData.card_label)
                jsonData.value.put("card_scheme_type", cardData.card_scheme_type)
                jsonData.value.put("pan_masking", cardControl.pan_masking)
                jsonData.value.put("host_record_index", cardData.host_record_index!!)
                Log.v("TEST", "host: $host")

                if (host != null) {
                    jsonData.value.put("tid", host.terminal_id)
                    jsonData.value.put("mid", host.merchant_id)
                    jsonData.value.put("nii", host.nii)
                    jsonData.value.put("stan", host.stan)
                    jsonData.value.put("ip_address1", host.ip_address1)
                    jsonData.value.put("port1", host.port1)
                    jsonData.value.put("host_define_type", host.host_define_type)
                    jsonData.value.put("host_label", host.host_label_name)
                    jsonData.value.put("batch_number", host.last_batch_number)
                    jsonData.value.put("invoice", getTraceInvoice())
                    returnData = true
                }
                else{
                    hasError!!.value = true
                    errorMessage!!.value =
                        "Not found host"
                    Log.v("TEST", "Not found host")
                }
            } else {
                hasError!!.value = true
                errorMessage!!.value =
                    "Not allow to ${jsonData.value.getString("transaction_type")}"
                Log.v("TEST", "not allow to ${jsonData.value.getString("transaction_type")}")
            }
        } else {
            hasError!!.value = true
            errorMessage!!.value = "Not found card control"
            Log.v("TEST", "not found card control")
        }
    } else {
        hasError!!.value = true
        errorMessage!!.value = "Card not support"
        Log.v("TEST", "not support")
    }
   return returnData
}

