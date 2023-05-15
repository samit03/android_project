package com.emc.edc.online_transaction

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import com.emc.edc.*
import com.emc.edc.screen.utils.getHostInformationFromCard
import com.emc.edc.screen.utils.getInformationTosaveTransaction
import com.emc.edc.utils.ISO8583Extracting
import com.emc.edc.utils.TransportData
import com.emc.edc.utils.buildISO8583
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.ArrayList

@SuppressLint("CoroutineCreationDuringComposition")
//@Composable
@RequiresApi(Build.VERSION_CODES.O)
@DelicateCoroutinesApi
suspend fun sendOnlineTransaction(
    context: Context,
    hasError: MutableState<Boolean>,
    errMessage: MutableState<String>,
    jsonData: MutableState<JSONObject>,
    responseOnlineStatus: MutableState<Boolean>,
    dialogStatus: MutableState<Boolean>,
    isConnectionTimeOut: MutableState<Boolean>,
    textLoading: MutableState<String>,
    popupPrintSlipCustomer: MutableState<Boolean>
) {
    try {
        val getHostInformationStatus = getHostInformationFromCard(
            jsonData,
            hasError,
            errMessage
        )
        if (getHostInformationStatus) {
            val cardData =
                selectCardData(jsonData.value.optString("card_number"))
            val host = selectHost(cardData!!.host_record_index!!)
            val dataISO8583 = buildISO8583(hasError, errMessage, jsonData.value)
            val requestToHost = dataISO8583.optString("iso8583_payload")
            val reversalMessage =
                dataISO8583.optString("iso8583_reversal_payload")
            var reversalFlag = host!!.reversal_flag!!
            var responseFromHost = ArrayList<String>()

            var loop = if (reversalFlag) {
                2
            } else {
                1
            }
            Log.v("LOOP : ", loop.toString())
            while (loop > 0) {
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

                if (!reversalFlag) { //send normal transaction
                    setReverseFlag(cardData!!.host_record_index!!, false)
                    saveReverseMassage(cardData!!.host_record_index!!, null)
                    textLoading.value = "Sending Transaction"
                    delay(1_500)
                    responseFromHost =
                        TransportData().sendOnlineTransaction(
                            jsonData.value,
                            requestToHost,
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
                    loop = 0

                } else { // communication success
                    var meaningShow = ""
                    val jsonResponse =
                        ISO8583Extracting().extractISO8583TOJSON(
                            responseFromHost
                        )

                    if (if (jsonResponse.has("res_code")) {
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

                        if (reversalFlag) { // if reversal has flag
                            reversalFlag = false
                        } else { // if reversal hasn't flag
                            jsonData.value.put("res_code", jsonResponse.optString("res_code"))
                            responseOnlineStatus.value = true
                            //dialogStatus.value = false
                            jsonData.value = getInformationTosaveTransaction(
                                jsonData.value,
                                jsonResponse
                            )

                            //dialogStatus.value = false
                            //val transaction = TransactionHistoryRO()
                            if (jsonData.value.optString("process") != "fullEMV") {
                                if (jsonData.value.optString("group") == "card_entry") {
                                    saveTransaction(jsonData.value)
                                } else {
                                    updateTransaction(jsonData.value)
                                }
                                printSlipTransaction(
                                    jsonData.value,
                                    context,
                                    hasError,
                                    errMessage
                                )
                                popupPrintSlipCustomer.value = true
                            }

                        }

                    } else { // host not approve
                        updateStan(cardData!!.host_record_index!!)
                        hasError.value = true
                        errMessage.value =
                            "Transaction not approve because:$meaningShow"
                    }
                }
                loop -= 1
            }
        }
    } catch (e: Exception) {
        hasError.value = true
        if (errMessage.value == "") {
            errMessage.value = e.toString()
        }
    }

}