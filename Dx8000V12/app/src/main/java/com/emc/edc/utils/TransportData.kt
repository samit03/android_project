package com.emc.edc.utils

import android.util.Log
import androidx.compose.runtime.MutableState
import com.emc.edc.selectHost
import kotlinx.coroutines.*
import kotlinx.coroutines.withContext
import org.json.JSONObject

class TransportData() {
    val utils = Utils()
    suspend fun sendOnlineTransaction(
        jsonData: JSONObject,
        isoMessage: String,
        isConnectionTimeOut: MutableState<Boolean>,
        hasError: MutableState<Boolean>,
        errMessage: MutableState<String>,
        textLoading: MutableState<String>,
    )
            : ArrayList<String>{
        var dataReturn: ArrayList<String> = ArrayList()


        if(isoMessage != ""){
            try {
                textLoading.value = "Connecting"
                val host =
                    withContext(Dispatchers.Default) { selectHost(jsonData.getInt("host_record_index")) }
                val socket = SocketCommunication(host!!.ip_address1!!, //"192.168.1.103",
                    host!!.port1!!, //7500,
                    10000)
                val hosConnected = withContext(Dispatchers.Default) { socket.run() }

                if(hosConnected){
                    textLoading.value = "Connected"
                    val request = withContext(Dispatchers.Default){socket.write(isoMessage)}

                    Log.v("TEST", "Request to host :$isoMessage")
                    if (request) {
                        textLoading.value = "Request to host"
                        var response = withContext(Dispatchers.Default) {socket.read() }
                        Log.v("TEST", "Response from host :$response")

                        if (response.size > 0) {
                            textLoading.value = "Response from host"
                            dataReturn = response
                        }
                        else{ //response = false
                            hasError.value = true
                            errMessage.value = "Response from host failed"
                            isConnectionTimeOut.value = true
                            return dataReturn
                        }
                    }
                    else{ //request = false
                        hasError.value = true
                        errMessage.value = "Request to host failed"
                        isConnectionTimeOut.value = true
                        return dataReturn
                    }

                }
                else{ // hosConnected = false
                    hasError.value = true
                    errMessage.value = "Connection to host failed"
                    //connectionStatus.value = true
                    //onlineStatus.value = true
                    return dataReturn
                }
            }
            catch (e: Exception) {
                //return "Connection error"
                //onlineStatus.value = true
                hasError.value = true
                errMessage.value = "Send data to online error : $e"
                return dataReturn
            }
        }
        return dataReturn
    }
}