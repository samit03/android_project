package com.emc.edc.online_transaction

import android.util.Log
import androidx.compose.runtime.MutableState
import com.emc.edc.*
import com.emc.edc.utils.ISO8583Extracting
import com.emc.edc.utils.TransportData
import com.emc.edc.utils.buildISO8583
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.ArrayList

suspend fun sendReversal(
    hasError: MutableState<Boolean>,
    errMessage: MutableState<String>,
    jsonData: MutableState<JSONObject>,
    responseOnlineStatus: MutableState<Boolean>,
    dialogStatus: MutableState<Boolean>,
    isConnectionTimeOut: MutableState<Boolean>,
    textLoading: MutableState<String>
) {
    try {
        //hasError.value = false
        //errMessage.value = ""
        responseOnlineStatus.value = false
        val cardData =
            selectCardData(jsonData.value.optString("card_number"))
        val host = selectHost(cardData!!.host_record_index!!)
        val dataISO8583 = buildISO8583(hasError, errMessage, jsonData.value)
        val reversalMessage =
            dataISO8583.optString("iso8583_reversal_payload")
        var reversalFlag = host!!.reversal_flag!!
        var responseFromHost = ArrayList<String>()
        //while (loop > 0) {
        dialogStatus.value = true
        if (reversalFlag) {//send reversal
            textLoading.value = "Sending reversal"
            delay(1_500)
            responseFromHost =
                TransportData().sendOnlineTransaction(
                    jsonData.value,
                    host.reversal_msg!!,
                    isConnectionTimeOut,
                    hasError,
                    errMessage,
                    textLoading
                )
        }
        if (hasError.value) { // communication error
            if (!reversalFlag &&
                isConnectionTimeOut.value
            ) {// if haven't flag
                setReverseFlag(cardData!!.host_record_index!!, true)
                saveReverseMassage(cardData!!.host_record_index!!, reversalMessage)
            }
            //hasError.value = false
            //errMessage.value = ""

        } else { // communication success
            var meaningShow = ""
            val jsonResponse =
                ISO8583Extracting().extractISO8583TOJSON(
                    responseFromHost
                )

            if (if (jsonResponse.has("res_code")) {
                    jsonData.value.put("res_code", jsonResponse.optString("res_code"))
                    val (isApprove, meaning) = ISO8583Extracting().checkBit39Payload(
                        jsonResponse.optString("res_code")
                    )
                    meaningShow = meaning.toString()
                    Log.v(
                        "TEST",
                        "Approve: $isApprove Meaning: $meaning"
                    )
                    textLoading.value = "Status is: $meaning"
                    delay(1_700)
                    val transactionIsApprove = isApprove as Boolean
                    transactionIsApprove

                } else {
                    false
                }
            ) { // host approve
                setReverseFlag(cardData!!.host_record_index!!, false)
                saveReverseMassage(cardData!!.host_record_index!!, null)
                updateStan(cardData!!.host_record_index!!)
                reversalFlag = false

            } else { // host not approve
                withContext(Dispatchers.Default) { updateStan(cardData!!.host_record_index!!) }
                hasError.value = true
                errMessage.value =
                    "Transaction not approve because:$meaningShow"
            }
        }
        //}


    } catch (e: Exception) {
        hasError.value = true
        if (errMessage.value == "") {
            errMessage.value = e.toString()
        }
    }
    responseOnlineStatus.value = true

}

fun prepareReversal(
    jsonData: MutableState<JSONObject>,
    hasError: MutableState<Boolean>,
    errMessage: MutableState<String>
) {
    val cardData =
        selectCardData(jsonData.value.optString("card_number"))
    val dataISO8583 =
        buildISO8583(hasError, errMessage, jsonData!!.value)
    val reversalMessage =
        dataISO8583.optString("iso8583_reversal_payload")
    setReverseFlag(cardData!!.host_record_index!!, true)
    saveReverseMassage(
        cardData!!.host_record_index!!,
        reversalMessage
    )

}
