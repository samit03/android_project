package com.emc.edc.emv.tag_end_process

import androidx.compose.runtime.MutableState
import com.emc.edc.emv.DeviceHelper
import com.emc.edc.utils.BytesUtil
import com.emc.edc.utils.Utils
import com.usdk.apiservice.aidl.emv.TransData
import com.usdk.apiservice.aidl.emv.UEMV
import org.json.JSONObject

fun getCommonData(transData: TransData?, jsonData: MutableState<JSONObject>? = null, ) {
    var emv: UEMV? = DeviceHelper.me().emv
    jsonData!!.value.put(
        "card_number",
        BytesUtil.bytes2HexString(transData!!.pan).replace("F", "")
    )
    jsonData!!.value.put(
        "card_exp",
        BytesUtil.bytes2HexString(transData!!.expiry).substring(2, 6)
    )
    val name = if (Utils().hexToAscii(emv!!.getTLV("5F20")).contains("/")) {
        val split_name = Utils().hexToAscii(emv!!.getTLV("5F20")).split("/")
        split_name[1].replace(" ", "") + " " + split_name[0].replace(
            "\\",
            ""
        )
    } else {
        Utils().hexToAscii(emv!!.getTLV("5F20")).replace("\\s+".toRegex(), " ")
    }
    jsonData!!.value.put("name", name)
    jsonData!!.value.put("app_name", Utils().hexToAscii(emv!!.getTLV("50")))
}