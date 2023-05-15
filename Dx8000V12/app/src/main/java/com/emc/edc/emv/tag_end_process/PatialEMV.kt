package com.emc.edc.emv.tag_end_process

import android.text.TextUtils
import androidx.compose.runtime.MutableState
import com.emc.edc.emv.DeviceHelper
import com.emc.edc.emv.util.TLV
import com.emc.edc.utils.BytesUtil
import com.usdk.apiservice.aidl.emv.UEMV
import org.json.JSONObject

fun getDataPatialEMV(jsonData: MutableState<JSONObject>? = null)  {
    var emv: UEMV? = DeviceHelper.me().emv
    var track2 = emv!!.getTLV("57")
    if (track2.length == 38) {
        track2 = track2.substring(0, track2.length - 1)
    }
    jsonData!!.value.put("track2", track2)
    var tlvResult = ""
    val tag = ("9F26,9F27,9F10,9F37,9F36,95,9F03,9F1E,9A,9C,9F02,5F2A," +
            "82,9F1A,9F33,9F34,9F35,84,9F09,9F41,5F34,5F28, 9F6E, 9F33")
    val tagArray = tag.split(",").toTypedArray()

    val tag9F02 = emv!!.getTLV("9F02")

    for (i in tagArray.indices) {
        val t = tagArray[i].trim { it <= ' ' }
        if (!TextUtils.isEmpty(t)) {
            if (emv!!.getTLV(t) != "") {
                tlvResult += TLV.fromData(
                    t,
                    BytesUtil.hexString2Bytes(emv!!.getTLV(t))
                )
                    .toString()
            }
        }
    }

    val paddingData = (tlvResult.length / 2).toString().padStart(4, '0')
    val tlvData = paddingData + tlvResult
    /**
     * put bit 55 to json to go online
     */
    jsonData!!.value.put("emv", tlvData)
}