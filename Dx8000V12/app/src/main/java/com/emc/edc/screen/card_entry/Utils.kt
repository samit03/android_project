package com.emc.edc.screen.card_entry

import android.content.Context
import androidx.compose.runtime.MutableState
import com.emc.edc.online_transaction.prepareReversal
import com.emc.edc.printSlipTransaction
import com.emc.edc.saveOfflineTransaction
import com.emc.edc.saveTransaction
import org.json.JSONObject

class Utils {
    fun cardEntryHandle(
        jsonData: MutableState<JSONObject>,
        requestOnlineStatus: MutableState<Boolean>,
        endProcessStatus: MutableState<Boolean>,
        errMessage: MutableState<String>,
        responseOnlineStatus: MutableState<Boolean>,
        hasError: MutableState<Boolean>,
        isSendReversal: MutableState<Boolean>,
        context: Context,
        dialogStatus: MutableState<Boolean>,
        textLoading: MutableState<String>,
        popupPrintSlipCustomer: MutableState<Boolean>,
        popUpContinue: MutableState<Boolean>,
        confirmAmountEnableStatus: MutableState<Boolean>,
        buttonConfirmAmount: MutableState<Boolean>
    ) {

        when (jsonData.value.optString("gen_ac")) {
            "AAC" -> {
                if (jsonData.value.optString("cancel_event") == "force_stop") {
                    if (jsonData.value.optString("process") == "partialEMV") {
                        requestOnlineStatus.value = true
                    } else if (jsonData.value.optString("operation") == "magnetic") {
                        endProcessStatus.value = true
                    }
                } else {
                    if (errMessage.value == "") {
                        errMessage.value = "EMV Decline"

                        if (responseOnlineStatus.value) {
                            if (jsonData!!.value.optString("process") == "fullEMV") {
                                prepareReversal(jsonData, hasError, errMessage)
                                isSendReversal.value = true
                            }
                        }
                        hasError.value = true
                    }
                }
            }
            else -> {
                if (jsonData.value.optString("transaction_status") == "offline") {
                    saveOfflineTransaction(
                        jsonData.value,
                        context,
                        hasError,
                        errMessage,
                        dialogStatus,
                        textLoading,
                        popupPrintSlipCustomer,
                        popUpContinue
                    )
                    printSlipTransaction(
                        jsonData.value,
                        context,
                        hasError,
                        errMessage
                    )
//                    popupPrintSlipCustomer.value = true
//                    endProcessStatus.value = true
                } else {
                    if (jsonData.value.optString("gen_ac") == "ARQC") {

                        if (jsonData.value.optString("process") == "partialEMV") {

                            if (!confirmAmountEnableStatus!!.value) {     // disable ui to display confirm amount
                                requestOnlineStatus!!.value =
                                    true        // request online immediately
                            } else {
                                buttonConfirmAmount!!.value =
                                    true        // enable ui to display confirm amount
                            }
                        } else {
                            requestOnlineStatus!!.value = true        // request online immediately
                        }
                    } else if (jsonData.value.optString("gen_ac") == "TC") {
                        textLoading.value = "EMV Approve"
//                    delay(1_500)
                        hasError.value = false
                        errMessage.value = ""
                        if (!responseOnlineStatus.value) {
                            saveOfflineTransaction(
                                jsonData.value,
                                context,
                                hasError,
                                errMessage,
                                dialogStatus,
                                textLoading,
                                popupPrintSlipCustomer,
                                popUpContinue
                            )
                        } else {
                            saveTransaction(jsonData.value)
                        }
                        printSlipTransaction(
                            jsonData.value,
                            context,
                            hasError,
                            errMessage
                        )
                        popupPrintSlipCustomer.value = true
                        endProcessStatus.value = true
                    }

                }
            }
        }
    }
}