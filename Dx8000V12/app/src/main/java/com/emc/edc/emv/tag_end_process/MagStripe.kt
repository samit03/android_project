package com.emc.edc.emv.tag_end_process

import androidx.compose.runtime.MutableState
import com.emc.edc.emv.DeviceHelper
import com.emc.edc.utils.Utils
import com.usdk.apiservice.aidl.emv.UEMV
import org.json.JSONObject

fun getDataMagStripe(jsonData: MutableState<JSONObject>? = null){
    var emv: UEMV? = DeviceHelper.me().emv
    jsonData!!.value = jsonData.value.put("operation", "mag_stripe")
    val track1 =
        Utils().hexToAscii(emv!!.getTLV("DF45")).replace("", "").replace("", "")
    var track2 =
        Utils().hexToAscii(emv!!.getTLV("DF46")).replace("", "").replace("", "")
//
    if (track2.length == 38) {
        track2 = track2.substring(0, track2.length - 1) //"" ""
    }
    jsonData!!.value.put("track1", track1)
    jsonData!!.value.put("track2", track2)
}