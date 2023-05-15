package com.emc.edc.emv

import android.os.Bundle
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import androidx.annotation.CallSuper
import androidx.compose.runtime.MutableState
import androidx.navigation.NavController
import com.emc.edc.constant.DemoConfig
import com.emc.edc.emv.entity.CardOption
import com.emc.edc.emv.entity.EMVOption
import com.emc.edc.emv.tag_end_process.getCommonData
import com.emc.edc.emv.tag_end_process.getDataMagStripe
import com.emc.edc.emv.tag_end_process.getDataPatialEMV
import com.emc.edc.emv.tag_finnal_select.*
import com.emc.edc.emv.util.EMVInfoUtil
import com.emc.edc.emv.util.EMVInfoUtil.getACTypeDesc
import com.emc.edc.emv.util.LogUtil
import com.emc.edc.emv.util.TLV
import com.emc.edc.emv.util.TLVList
import com.emc.edc.getAID
import com.emc.edc.getAIDList
import com.emc.edc.getCAPK
import com.emc.edc.getDataSetting
import com.emc.edc.pinpad.BasePinpad
import com.emc.edc.utils.BytesUtil
import com.emc.edc.utils.Utils
import com.usdk.apiservice.aidl.constants.RFDeviceName
import com.usdk.apiservice.aidl.data.StringValue
import com.usdk.apiservice.aidl.emv.*
import com.usdk.apiservice.aidl.magreader.MagData
import com.usdk.apiservice.aidl.magreader.TrackID
import com.usdk.apiservice.aidl.pinpad.*
import org.json.JSONObject
import java.util.*


open class Emv(
    val jsonData: MutableState<JSONObject>? = null,
    private var hasError: MutableState<Boolean>? = null,
    private var errorMessage: MutableState<String>? = null,
    val navController: NavController? = null,
    private val seletAIDPopup: MutableState<Boolean>? = null,
    private val aidList: MutableList<String>? = null,
    private val aidOriginalList: MutableList<List<CandidateAID>>? = null,
    private val buttonConfirmAmount: MutableState<Boolean>? = null,
    private val checkEMVProcess: MutableState<Boolean>? = null,
    private val confirmAmountEnableStatus: MutableState<Boolean>? = null,
    private val endProcessStatus: MutableState<Boolean>? = null,
    private val hasEMVStartAgain: MutableState<Boolean>? = null,
    private val countTryAgain: MutableState<Int>? = null,
    private val hasEMVStartAgainConfirm: MutableState<Boolean>? = null,
) {
    private var emvOption = EMVOption.create()
    private var cardOption = CardOption.create()
    private var emv: UEMV? = DeviceHelper.me().emv;
    private var lastCardRecord: CardRecord? = null
    private var wholeTrkId = 0
    private val processOptimization = false
    private var kernel = ""
    private val utils = Utils()
    private val numStartEMV = 3
    private val pinpad: UPinpad? = DeviceHelper.me()
        .getPinpad(
            KAPId(0, 0),
            KeySystem.KS_MKSK,
            DeviceName.IPP
        ).also {
            pinpad = it
        }


    @CallSuper //@Override
    protected fun onCreateView(savedInstanceState: Bundle?) {
        initDeviceInstance()
        //setContentView(R.layout.activity_emv);
        initCardOption()
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }


    private fun initDeviceInstance() {
        emv = DeviceHelper.me().emv
    }

    private fun openPinpad() {
        try {
            pinpad!!.open()
        } catch (e: RemoteException) {
            e.printStackTrace()
            hasError!!.value = true
            errorMessage!!.value = "${e.message}"
        }
    }

    private fun closePinpad() {
        try {
            pinpad!!.close()
        } catch (e: RemoteException) {
            e.printStackTrace()
            hasError!!.value = true
            errorMessage!!.value = "${e.message}"
        }
    }

    private fun initCardOption() {
        val dataSetting = getDataSetting()
        setTrkIdWithWholeData(false, TrackID.TRK1)
        setTrkIdWithWholeData(false, TrackID.TRK2)
        setTrkIdWithWholeData(false, TrackID.TRK2)
        var fallBackStatus = if (jsonData!!.value.has("operation"))
            jsonData!!.value.optString("operation") == "fall_back"
        else false

        if (fallBackStatus) {
            cardOption.supportICCard(false)
            cardOption.supportMagCard(true)
            cardOption.supportRFCard(false)
        } else {
            cardOption.supportICCard(dataSetting!!.allow_insert_card)
            cardOption.supportMagCard(dataSetting!!.allow_swipe_card)
            cardOption.supportRFCard(dataSetting!!.allow_pass_card)
        }

        cardOption.supportAllRFCardTypes(false)
        cardOption.rfDeviceName(RFDeviceName.INNER)
        cardOption.trackCheckEnabled(false)
    }

    private fun setTrkIdWithWholeData(isSlted: Boolean, trkId: Int) {
        wholeTrkId = if (isSlted) {
            wholeTrkId or trkId
        } else {
            wholeTrkId and trkId.inv()
        }
        cardOption.trkIdWithWholeData(wholeTrkId)
    }

    private fun searchRFCard(next: Runnable) {
        val rfCardOption = CardOption.create()
            .supportICCard(true)
            .supportMagCard(false)
            .supportRFCard(true)
            .rfDeviceName(DemoConfig.RF_DEVICE_NAME)
            .toBundle()
        try {
            emv!!.searchCard(rfCardOption, DemoConfig.TIMEOUT, object : SearchListenerAdapter() {
                override fun onCardPass(cardType: Int) {
                    next.run()
                }

                override fun onTimeout() {
                    stopEMV()
                }

                override fun onError(code: Int, message: String) {
                    stopEMV()
                    emv!!.stopSearch()
                    if (message == "Card timeout") {
                        Log.d("EMV Error", message)
                        //stopEMV()
                        startEMV()
                        "TODO()"
                    } else {
                        hasError!!.value = true
                        errorMessage!!.value = message
                    }
                }
            })
        } catch (e: Exception) {
            Log.d("EMV Error", "Read Card Error Exception")
            hasError!!.value = true
            errorMessage!!.value = "${e.message}"
        }
    }

    private fun searchCard(next: Runnable) {
        try {
            val cardHandler = object : SearchCardListener.Stub() {
                override fun onCardPass(cardType: Int) {
                    jsonData!!.value.put("operation", "contactless")
                    jsonData.value.put("pos_entry_mode", "050")

                    if (!jsonData!!.value.has("process")) {
                        jsonData!!.value.put("process", "partialEMV")
                    }
                    next.run()
                }

                override fun onCardInsert() {
                    jsonData!!.value.put("operation", "contact")
                    jsonData!!.value.put("pos_entry_mode", "052")
                    if (!jsonData!!.value.has("process")) {
                        jsonData!!.value.put("process", "fullEMV")
                    }
                    next.run()
                }

                override fun onCardSwiped(track: Bundle?) {
                    try {
//                        if (!jsonData!!.value.has("process")) {
//                            jsonData!!.value.put("process", "partialEMV")
//                        }
                        val currentDate = "${Date().year - 100}" +
                                "${if (1 + Date().month < 10) "0${1 + Date().month}" else 1 + Date().month}" +
                                "${if (Date().date < 10) "0${Date().date}" else Date().date}"

                        val currentTime =
                            Date().hours.toString().padStart(2, '0') + Date().minutes.toString()
                                .padStart(2, '0') + Date().seconds.toString()
                                .padStart(2, '0')
                        jsonData!!.value.put("date", currentDate)
                        jsonData!!.value.put("time", currentTime)
                        if (track!!.getString("TRACK1") != "") {
                            val split_track1 = track!!.getString(MagData.TRACK1)!!.split("^")
                            jsonData!!.value.put("operation", "magnetic")
                            jsonData.value.put("pos_entry_mode", "022")
                            // operation!!.value = "magnetic"
                            jsonData!!.value.put("track3", track!!.getString("TRACK3"))
                            jsonData!!.value.put(
                                "card_number",
                                track!!.getString("PAN")!!.replace("F", "")
                            )
                            jsonData!!.value.put("track1", track.getString("TRACK1"))
                            jsonData!!.value.put("card_exp", track.getString("EXPIRED_DATE"))
                            jsonData!!.value.put("track2", track.getString("TRACK2"))
                            jsonData!!.value.put("Service code:", track.getString("SERVICE_CODE"))
                            val name = if (split_track1[1].contains("/")) {
                                val split_name = split_track1[1].split("/")
                                split_name[1].replace(" ", "") + " " + split_name[0].replace(
                                    "\\",
                                    ""
                                )
                            } else {
                                split_track1[1].replace("\\s+".toRegex(), " ")
                            }
                            jsonData!!.value.put("name", name)
                            jsonData!!.value.put("gen_ac", "ARQC")
                            if (!confirmAmountEnableStatus!!.value) {
                                checkEMVProcess!!.value = true
                            }
                            buttonConfirmAmount!!.value = true

                        } else {
                            if (countTryAgain!!.value <= numStartEMV) {
                                hasError!!.value = true
                                hasEMVStartAgain!!.value = true
                                errorMessage!!.value = "Magnetic card error, Please try again"
                            } else {
                                hasError!!.value = true
                                errorMessage!!.value = "Magnetic card error"
                                hasEMVStartAgain!!.value = false

                            }
                        }
                    } catch (e: Exception) {
                        hasError!!.value = true
                        errorMessage!!.value = e.toString()
                    }
                    //stopEMV()
                }

                override fun onTimeout() {
                    stopEMV()
                }

                override fun onError(code: Int, message: String) {
                    if (message == "Card timeout") {
                        Log.d("EMV Error", message)
                        //stopEMV()
                        startEMV()
                        "TODO()"
                    } else {
                        errorMessage!!.value = message
                        hasError!!.value = true
                        stopEMV()
                    }
                }
            }
            emv?.searchCard(
                cardOption.toBundle(),
                60,
                cardHandler
            )
        } catch (e: Exception) {
            Log.e("EMV SearchCardListener", e.message.toString())
            errorMessage!!.value = "${e.message}"
            hasError!!.value = true
        }
    }

    fun startEMV() {
        try {
            //initDeviceInstance()
            //emv = DeviceHelper.me().emv
            initCardOption()
            Log.d("EMV", "Start EMV")

            //getKernelVersion();
            //getCheckSum();
            emv!!.startEMV(emvOption.toBundle(), emvEventHandler)
            openPinpad()
        } catch (e: Exception) {
            Log.d("EMV", "Start EMV Error")
            errorMessage!!.value = "${e.message}"
            hasError!!.value = true
        }
    }

    private fun getKernelVersion() {
        try {
            val version = StringValue()
            val ret = emv!!.getKernelVersion(version)
            if (ret == EMVError.SUCCESS) {
                //outputBlackText("EMV kernel version: " + version.getData());
            } else {
                //outputRedText("EMV kernel version: fail, ret = " + ret);
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
            errorMessage!!.value = "${e.message}"
            hasError!!.value = true
        }
    }

    private fun getCheckSum() {
        try {
            val flag = 0xA2
            val checkSum = StringValue()
            val ret = emv!!.getCheckSum(flag, checkSum)
            if (ret == EMVError.SUCCESS) {
                //outputBlackText("EMV kernel[" + flag + "] checkSum: " + checkSum.getData());
            } else {
                //outputRedText("EMV kernel[" + flag + "] checkSum: fail, ret = " + ret);
            }
        } catch (e: RemoteException) {
            errorMessage!!.value = "${e.message}"
            hasError!!.value = true
            e.printStackTrace()
        }
    }

    fun stopEMV() {
        try {
            emv!!.stopEMV()
            emv!!.stopSearch()
            emv!!.stopProcess()
            closePinpad()
        } catch (e: Exception) {
            Log.e("test aa3a", e.message.toString())
            errorMessage!!.value = "${e.message}"
            hasError!!.value = true
        }
    }

    protected fun stopSearch() {
        try {
            emv!!.stopSearch()
        } catch (e: Exception) {
            errorMessage!!.value = "${e.message}"
            hasError!!.value = true
        }
    }

    private fun halt() {
        try {
            emv!!.halt()
        } catch (e: Exception) {
            errorMessage!!.value = "${e.message}"
            hasError!!.value = true
        }
    }

    private var emvEventHandler: EMVEventHandler = object : EMVEventHandler.Stub() {
        @Throws(RemoteException::class)
        override fun onInitEMV() {
            doInitEMV()
        }

        @Throws(RemoteException::class)
        override fun onWaitCard(flag: Int) {
            doWaitCard(flag)
        }

        @Throws(RemoteException::class)
        override fun onCardChecked(cardType: Int) {
            // Only happen when use startProcess()
            doCardChecked(cardType)
        }

        @Throws(RemoteException::class)
        override fun onAppSelect(reSelect: Boolean, list: List<CandidateAID>) {
            doAppSelect(reSelect, list)
        }

        @Throws(RemoteException::class)
        override fun onFinalSelect(finalData: FinalData) {
            doFinalSelect(finalData)
        }

        @Throws(RemoteException::class)
        override fun onReadRecord(cardRecord: CardRecord) {
            lastCardRecord = cardRecord
            doReadRecord(cardRecord)
        }

        @Throws(RemoteException::class)
        override fun onCardHolderVerify(cvmMethod: CVMMethod) {
            doCardHolderVerify(cvmMethod)
        }

        @Throws(RemoteException::class)
        override fun onOnlineProcess(transData: TransData?) {
            doOnlineProcess(transData)
        }

        @Throws(RemoteException::class)
        override fun onEndProcess(result: Int, transData: TransData) {
            doEndProcess(result, transData)
        }

        @Throws(RemoteException::class)
        override fun onVerifyOfflinePin(
            flag: Int,
            random: ByteArray,
            caPublicKey: CAPublicKey,
            offlinePinVerifyResult: OfflinePinVerifyResult
        ) {
            doVerifyOfflinePin(flag, random, caPublicKey, offlinePinVerifyResult)
        }

        @Throws(RemoteException::class)
        override fun onObtainData(ins: Int, data: ByteArray) {
            //outputText("=> onObtainData: instruction is 0x" + Integer.toHexString(ins) + ", data is " + BytesUtil.bytes2HexString(data));
        }

        @Throws(RemoteException::class)
        override fun onSendOut(ins: Int, data: ByteArray) {
            doSendOut(ins, data)
        }
    }

    @Throws(RemoteException::class)
    fun doInitEMV() {

        //outputText("=> onInitEMV ");
        manageAID()
        //  init transaction parameters，please refer to transaction parameters
        //  chapter about onInitEMV event in《UEMV develop guide》
        //  For example, if VISA is supported in the current transaction,
        //  the label: DEF_TAG_PSE_FLAG(M) must be set, as follows:
        emv!!.setTLV(KernelID.VISA, EMVTag.DEF_TAG_PSE_FLAG, "03")
        // For example, if AMEX is supported in the current transaction，
        // labels DEF_TAG_PSE_FLAG(M) and DEF_TAG_PPSE_6A82_TURNTO_AIDLIST(M) must be set, as follows：
        // emv.setTLV(KernelID.AMEX, EMVTag.DEF_TAG_PSE_FLAG, "03");
        // emv.setTLV(KernelID.AMEX, EMVTag.DEF_TAG_PPSE_6A82_TURNTO_AIDLIST, "01");
        val getStatus = emv!!.controlAID(ActionFlag.ADD, "A000000677010", 3, 7)
        val test1 = getStatus
    }

    @Throws(RemoteException::class)
    protected fun manageAID() {

        val getAID = getAIDList()

        if (getAID != null) {
            for (aid in getAID) {
                val ret = emv!!.manageAID(ActionFlag.ADD, aid.aid, true)
            }
        } else {
            hasError!!.value = true
            errorMessage!!.value = "The terminal hasn't AID"
        }

    }

    @Throws(RemoteException::class)
    fun doWaitCard(flag: Int) {
        emv!!.stopSearch()
        when (flag) {
            WaitCardFlag.NORMAL -> searchCard(Runnable {
                if (processOptimization) {
                    return@Runnable
                }
                respondCard()
            })
            WaitCardFlag.ISS_SCRIPT_UPDATE, WaitCardFlag.SHOW_CARD_AGAIN -> searchRFCard { respondCard() }
            WaitCardFlag.EXECUTE_CDCVM -> emv!!.halt()
            else -> {}
        }
    }

    private fun respondCard() {
        try {
            emv!!.respondCard()
        } catch (e: RemoteException) {
            Log.e("test aa3a", e.message.toString())
            errorMessage!!.value = "${e.message}"
            hasError!!.value = true
        }
    }

    fun doCardChecked(cardType: Int) {
        // Only happen when use startProcess()
    }

    /**
     * Request cardholder to select application
     */
    fun doAppSelect(reSelect: Boolean, candList: List<CandidateAID>) {
        //outputText("=> onAppSelect: cand AID size = " + candList.size());
        if (candList.size > 1) {
            aidOriginalList!!.add(candList)
            for (candAid in candList) {
                aidList!!.add(String(candAid.appLabel))
            }
            seletAIDPopup!!.value = true
        } else {
            respondAID(candList[0].aid)
        }
    }


    fun selectedAID(select: Int, aidOriginalList: MutableList<List<CandidateAID>>) {
        respondAID(aidOriginalList[0][select].aid)
    }

    private fun respondAID(aid: ByteArray?) {
        try {
            //outputBlueText("Select aid: " + BytesUtil.bytes2HexString(aid));
            val tmAid = TLV.fromData(EMVTag.EMV_TAG_TM_AID, aid)
            emv!!.respondEvent(tmAid.toString())
        } catch (e: Exception) {
            Log.e("test aa3a", e.message.toString())
            errorMessage!!.value = "${e.message}"
            hasError!!.value = true
        }
    }

    /**
     * Parameters can be set or adjusted according to the aid selected finally
     * please refer to transaction parameters chapter about onFinalSelect event in《UEMV develop guide》
     */
    @Throws(RemoteException::class)
    fun doFinalSelect(finalData: FinalData) {
        var tlvList = ""
        val tagAidData = getAID(BytesUtil.bytes2HexString(finalData.aid))
        if (tagAidData == null) {
            hasError!!.value = true
            errorMessage!!.value = "Terminal hasn't AID"
        } else {
            //Common tag

            tlvList = ConmonTag(hasError, errorMessage, jsonData, tagAidData)
            when (finalData.kernelID) {
                KernelID.EMV.toByte() -> {
                    tlvList += EmvTag(jsonData, hasError, errorMessage, tagAidData)
                }

                KernelID.PBOC.toByte() -> {
                    tlvList +=
                        ("9F0206000000000100") +
                                ("9A03171020") +
                                ("9F2103150512") +
                                ("9F41040000000F") +
                                ("9F3303E0F8C8") +
                                ("9F1A020156") +
                                ("5F2A020156") +
                                ("9C0100") +
                                ("9F09020030") +
                                ("9F1B0400003A98") +
                                ("DF690100") +
                                ("9F660466004080") +
                                ("DF812406000000100000") +
                                ("DF812306000000100000") +
                                ("DF812606000000100000") +
                                ("DF06027C00")

                }
                KernelID.VISA.toByte() -> {
                    tlvList += VisaTag(hasError, errorMessage, tagAidData)
                }

                KernelID.MASTER.toByte() -> {
                    tlvList += MasterTag(hasError, errorMessage, tagAidData)
                }

                KernelID.AMEX.toByte() -> {
                    tlvList += AmexTag(hasError, errorMessage, tagAidData)
                }

                KernelID.DISCOVER.toByte() -> {}
                KernelID.JCB.toByte() -> {
                    tlvList += JcbTag(hasError, errorMessage, tagAidData)
//                    tlvList =
//                        "9F02060000000001009C01009F3501229F1A0201565F2A0201565F3601029A031710209F2103150512"+
//                        "DF918404027B00DF91840803708000DF918111050410000000DF918112059060009000DF918110059040008000"+
//                        "9F01060000000000109F150270329F4E175858204D45524348414E54205959204C4F434154494F4E"+
//                        "DF91840206000000020000DF91840306000000010000DF91840106000000004500DF9184050100DF9184060100DF91840904000007D0"

                }
                else -> {
                    errorMessage!!.value = "The terminal hasn't kernel"
                    hasError!!.value = true
                }
            }
        }
        var result = outputResult(
            emv!!.setTLVList(finalData.kernelID.toInt(), tlvList),
            "...onFinalSelect: setTLVList"
        )
        val output = outputResult(emv!!.respondEvent(null), "...onFinalSelect: respondEvent")
        val test1 = result
        val test2 = output
    }

    /**
     * Application to process card record data and set parameters
     * such as display card number, find blacklist, set public key, etc
     */
    @Throws(RemoteException::class)
    fun doReadRecord(record: CardRecord?) {
        jsonData!!.value.put(
            "card_number",
            BytesUtil.bytes2HexString(record!!.pan).replace("F", "")
        )
        jsonData!!.value.put("card_exp", BytesUtil.bytes2HexString(record!!.expiry).substring(2, 6))


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
        jsonData!!.value.put("emv_event", "read_record")



        val capkIndex = record?.pubKIndex?.let { byteArrayOf(it) }
        if (capkIndex != null) {
            Log.d("test aaa capk index", "=> ${capkIndex.toHex()}")
        }
        Log.d("test aaa aid", "=> ${BytesUtil.bytes2HexString(record?.aid)}")
        Log.d("test aaa algo ID", "=> ${record?.algorithmID}")
        Log.d("test aaa", "=> onFinalSelect ${EMVInfoUtil.getRecordDataDesc(record)}")

        Log.d(
            "test rid",
            "test rid => ${BytesUtil.bytes2HexString(record?.aid).subSequence(0, 10).toString()}"
        )

        val listPubKey = getCAPK(
            BytesUtil.bytes2HexString(record?.aid).subSequence(0, 10).toString(),
            capkIndex!!.toHex()
        )
        Log.d("test list pub", "test rid => $listPubKey")

        if (listPubKey != null) {
            val capKey = CAPublicKey()
            capKey.index = record!!.pubKIndex
            capKey.rid = listPubKey.rid!!.decodeHex()
            capKey.exp = listPubKey.exponent!!.decodeHex()
            capKey.mod = listPubKey.mod!!.decodeHex()
            capKey.hashFlag = 0x00.toByte()
            capKey.expDate = listPubKey.exp!!.decodeHex()
            capKey.hash = listPubKey.sha1!!.decodeHex()
            Log.d("Test", "get rid ${capKey.rid.toHex()}")
            Log.d("Test", "get mod ${capKey.mod.toHex()}")
            Log.d("Test", "get exp ${capKey.exp.toHex()}")
            Log.d("Test", "get expDate ${capKey.expDate.toHex()}")

            val ret = emv!!.setCAPubKey(capKey)
            Log.d(
                "test capk",
                "$ret => add CAPKey rid = : "
            )

            Log.d("test rid", "test rid => $capKey")
        }

        val tagStr =
            "4f,50,57, 5a,71, 72, 82,84,8a,8e,  91,  95,9a,  99,  9b , 9c  ,5f20  ,5f24  ,5f28," +
                    "  5f2a,  5f34 , 5f2d,  9f02 , 9f03 , 9f06 , 9f07,  9f08 , 9f09 , 9f0d , 9f0e ," +
                    " 9f0f,  9f10 , 9f11,  9f12,  9f14, 9f17,  9f1a,  9f1b , 9f1e , 9f1f , 9f21, " +
                    " 9f26  ,  9f27 ,   9f33,  9f34,    9f35,   9f36 ,  9f37, 9f39 ,  9f40 , 9f41, " +
                    " 9f42 , 9f53 , 9f5b,  9f5d , 9f67 , 9f6e , 9f71,  9f7c,  df918110,  df918111,  " +
                    "df918112 , df918124,  df30 , df32  ,df34  ,df35  ,df36 , df37 , df38 , df39"

        val tagArray = tagStr.split(",").toTypedArray()
        val tags: MutableList<String> = ArrayList()
        for (i in tagArray.indices) {
            val t = tagArray[i].trim { it <= ' ' }
            if (!TextUtils.isEmpty(t)) {
                tags.add(t)
            }
        }
        val list: List<TlvResponse> = ArrayList()
        //int ret = emv.getKernelDataList(tags, list);
        //LogUtil.d("getKernelDataList ret = " + ret);
        for (i in list.indices) {
            val info = list[i]
            LogUtil.d(
                "i = " + i + ", " + BytesUtil.bytes2HexString(info.tag) + ", ret = "
                        + info.result + ", " + BytesUtil.bytes2HexString(
                    info.value
                )
            )
        }
        if(jsonData.value.optString("process") == "fullEMV"){
            if (!confirmAmountEnableStatus!!.value){
                emv!!.respondEvent(null)
            }else{
                buttonConfirmAmount!!.value = true
            }
        }
        else if(jsonData.value.optString("process") == "partialEMV") {
            if(jsonData.value.optString("operation") == "contact"){
                if (!confirmAmountEnableStatus!!.value){
                    emv!!.stopEMV()
                }else{
                    buttonConfirmAmount!!.value = true
                }
            }else{
                readRecord()
            }

        }


//        if (!confirmAmountEnableStatus!!.value
//            || jsonData.value.optString("process") == "fullEMV"
//        ) {
//            emv!!.respondEvent(null)
//        } else if(jsonData.value.optString("process") == "partialEMV"){
//            emv!!.stopEMV()
//        }
//        else{
//            buttonConfirmAmount!!.value = true
//        }
    }

    fun readRecord() {
        outputResult(emv!!.respondEvent(null), "...onReadRecord: respondEvent")
    }


    /**
     * Request the cardholder to perform the Cardholder verification specified by the kernel.
     */
    @Throws(RemoteException::class)
    fun doCardHolderVerify(cvm: CVMMethod) {
        //outputText("=> onCardHolderVerify | " + EMVInfoUtil.getCVMDataDesc(cvm));
        val getCVMResult = EMVInfoUtil.getCVMDataDesc(cvm)
        val param = Bundle()
        param.putByteArray(PinpadData.PIN_LIMIT, byteArrayOf(0, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        val listener: OnPinEntryListener = object : OnPinEntryListener.Stub() {
            override fun onInput(arg0: Int, arg1: Int) {}
            override fun onConfirm(arg0: ByteArray, arg1: Boolean) {
                respondCVMResult(1.toByte())
                var pinData = BytesUtil.bytes2HexString(arg0)
                var test = Utils().hexToAscii(pinData)
            }

            override fun onCancel() {

                respondCVMResult(0.toByte())
                emv!!.stopProcess()
                emv!!.stopEMV()
                pinpad!!.close()
            }

            override fun onError(error: Int) {
                respondCVMResult(2.toByte())
                hasError!!.value = true
                errorMessage!!.value = BasePinpad().getErrorMessage(error).toString()

            }
        }
        when (cvm.cvm) {
            CVMFlag.EMV_CVMFLAG_OFFLINEPIN.toByte() -> {
                val cvmFlagValue = BytesUtil.hexString2Bytes(
                    emv!!.getTLV(EMVTag.DEF_TAG_CVM_FLAG)
                )
                //Offline ciphertext pin
                if (cvmFlagValue != null && cvmFlagValue.size > 0 && cvmFlagValue[0].toInt() == 0x31) {
                    val cslValue = BytesUtil.hexString2Bytes(
                        emv!!.getTLV("DF91815D")
                    )
                    //The public key recovery fails
                    if (cslValue != null && cslValue.size > 1 && cslValue[1].toInt() and 0x40 == 0x40) {
                        // In the case of offline ciphertext pin, if the public key recovery fails, it shall be applied without pop-up encryption window.
                        // Only the modular version of the kernel is supported
                        val chvStatus = TLV.fromData(EMVTag.DEF_TAG_CHV_STATUS, byteArrayOf(0x01))
                        emv!!.respondEvent(chvStatus.toString())
                        return
                    }
                }
                pinpad!!.startOfflinePinEntry(param, listener)
            }
            CVMFlag.EMV_CVMFLAG_ONLINEPIN.toByte() -> {
                //outputText("=> onCardHolderVerify | onlinpin");
                param.putByteArray(PinpadData.PAN_BLOCK, lastCardRecord!!.pan)
                pinpad!!.startPinEntry(DemoConfig.KEYID_PIN, param, listener)
            }
            else ->            //outputText("=> onCardHolderVerify | default");
                respondCVMResult(1.toByte())
        }
    }

    protected fun respondCVMResult(result: Byte) {
        try {
            val chvStatus = TLV.fromData(EMVTag.DEF_TAG_CHV_STATUS, byteArrayOf(result))
            val ret = emv!!.respondEvent(chvStatus.toString())
        } catch (e: Exception) {
            //handleException(e);
            hasError!!.value = true
            errorMessage!!.value = "${e.message}"
        }
    }

    /**
     * Request the application to execute online authorization.
     */
    @Throws(RemoteException::class)
    open fun doOnlineProcess(transData: TransData?) {
        var getACType = getACTypeDesc(transData!!.acType)
        jsonData!!.value.put("gen_ac", getACType)
        val tag77 = emv!!.getTLV("9F27")
        val tag80 = emv!!.getTLV("9F26")
        val getTVR = emv!!.getTLV("95")
        val tag50 = emv!!.getTLV("50")
        val getTranSequentCounter = emv!!.getTLV("9F41")
        val getTLV = BytesUtil.bytes2HexString(transData.tlvData)
        val getCurrencyCode = emv!!.getTLV("5F28")
        val test11 = getCurrencyCode

        var tlvResult = ""
        val tag = ("9F1E,9F26,9F27,9F10,9F37,9F36,95,9F03,9F1E,9A,9C,9F02,5F2A," +
                "82,9F1A,9F33,9F34,9F35,84,9F09,9F41,5F34,5F28")
        val tagArray = tag.split(",").toTypedArray()

        val tag9F02 = emv!!.getTLV("9F02")
        var test = ""

        for (i in tagArray.indices) {
            val t = tagArray[i].trim { it <= ' ' }
            if (!TextUtils.isEmpty(t)) {
                if (emv!!.getTLV(t) != "") {
                    tlvResult += TLV.fromData(t, BytesUtil.hexString2Bytes(emv!!.getTLV(t)))
                        .toString()
                }
            }
        }

        val paddingData = (tlvResult.length / 2).toString().padStart(4, '0')
        val tlvData = paddingData + tlvResult
        /**
         * put bit 55 to json to go online
         */
        jsonData!!.value.put("app_name", Utils().hexToAscii(emv!!.getTLV("50")))
        jsonData!!.value.put("emv", tlvData)
        checkEMVProcess!!.value = true
    }

    fun secondGenAC(jsonData: JSONObject) {
        val onlineResult = doOnlineProcess(jsonData)
        emv!!.respondEvent(onlineResult)
    }

    /**
     * pack message, communicate with server, analyze server response message.
     *
     * @return result of online process，he data elements are as follows:
     * DEF_TAG_ONLINE_STATUS (M)
     * If online communication is success, following is necessary while retured by host service.
     * EMV_TAG_TM_ARC (C)
     * DEF_TAG_AUTHORIZE_FLAG (C)
     * EMV_TAG_TM_AUTHCODE (C)
     * DEF_TAG_HOST_TLVDATA (C)
     */
    private fun doOnlineProcess(jsonData: JSONObject): String {
        var responseCode = ""
        var onlineSuccess = false
        var onlineApproved = false
        Log.d("Test Count", "1")

        if (jsonData!!.has("res_code")) {
            responseCode = utils.asciiToHex(jsonData.optString("res_code"))
            if (responseCode == "3030") {
                onlineApproved = true
            }
            onlineSuccess = true
        }
        return if (onlineSuccess) {
            Log.d("EMV", "Online Success")
            val onlineResult = StringBuffer()
            onlineResult.append(EMVTag.DEF_TAG_ONLINE_STATUS).append("01").append("00")
            val hostRespCode = responseCode
            onlineResult.append(EMVTag.EMV_TAG_TM_ARC).append("02").append(hostRespCode)
            onlineResult.append(EMVTag.DEF_TAG_AUTHORIZE_FLAG).append("01")
                .append(if (onlineApproved) "01" else "00")
            val hostTlvData =
                "9F3501229C01009F3303E0F1C89F02060000000000019F03060000000000009F101307010103A0A802010A010000000052856E2C9B9F2701809F260820F63D6E515BD2CC9505008004E8009F1A0201565F2A0201569F360201C982027C009F34034203009F37045D5F084B9A031710249F1E0835303530343230308408A0000003330101019F090200309F410400000001"
            onlineResult.append(
                TLV.fromData(
                    EMVTag.DEF_TAG_HOST_TLVDATA,
                    BytesUtil.hexString2Bytes(hostTlvData)
                ).toString()
            )
            onlineResult.toString()

        } else {
            //outputRedText("!!! online failed !!!");
            Log.d("EMV", "Online Error")
            "DF9181090101"

        }
    }

    fun doVerifyOfflinePin(
        flag: Int,
        random: ByteArray?,
        capKey: CAPublicKey?,
        result: OfflinePinVerifyResult
    ) {
        //outputText("=> onVerifyOfflinePin");
        try {
            /** inside insert card - 0；inside swing card – 6；External device is connected to the USB port - 7；External device is connected to the COM port -8  */
            val icToken = 0
            //Specify the type of "PIN check APDU message" that will be sent to the IC card.Currently only support VCF_DEFAULT.
            val cmdFmt = OfflinePinVerify.VCF_DEFAULT
            val offlinePinVerify = OfflinePinVerify(flag.toByte(), icToken, cmdFmt, random)
            val pinVerifyResult = PinVerifyResult()
            val ret = pinpad!!.verifyOfflinePin(
                offlinePinVerify,
                getPinPublicKey(capKey),
                pinVerifyResult
            )
            if (!ret) {
                //outputRedText("verifyOfflinePin fail: " + pinpad.getLastError());
                stopEMV()
                return
            }
            val apduRet = pinVerifyResult.apduRet
            val sw1 = pinVerifyResult.sW1
            val sw2 = pinVerifyResult.sW2
            result.setSW(sw1.toInt(), sw2.toInt())
            result.result = apduRet.toInt()
        } catch (e: Exception) {
            //handleException(e);
        }
    }

    /**
     * Inform the application that the EMV transaction is completed and the kernel exits.
     */
    fun doEndProcess(result: Int, transData: TransData?) {
        var acType = getACTypeDesc(transData!!.acType)
        jsonData!!.value.put("gen_ac", acType)
//        jsonData!!.value.put("gen_ac", "AAC")
//        val getCurrencyCode = emv!!.getTLV("5F28")
//        val tag77 = emv!!.getTLV("9F27")
//        val tag80 = emv!!.getTLV("9F26")
//        val tag9f34 = emv!!.getTLV("9F34")
//        val tag8E = emv!!.getTLV("8E")
        val tag50 = emv!!.getTLV("50")
//        val getTransaction = EMVInfoUtil.getTransDataDesc(transData)
        //        val test11 = getCurrencyCode
        //acType = "AAC"
        //jsonData!!.value.put("gen_ac", "AAC")
        val getFlow = EMVInfoUtil.getFlowTypeDesc(
            transData.flowType)
        val getCVM = EMVInfoUtil.getCVMDesc(transData.cvm)
        Log.d("Get AC Type", acType)
        if (result != EMVError.SUCCESS) {
            if (result == EMVError.ERROR_EMV_RESULT_STOP) {
                jsonData.value.put("cancel_event", "force_stop")
                checkEMVProcess!!.value = true
            } else if (result == EMVError.ERROR_POWERUP_FAIL) {
                hasError!!.value = true
                if (countTryAgain!!.value <= numStartEMV) {
                    hasEMVStartAgain!!.value = true
                    errorMessage!!.value = "IC card reading error, Please retry"
                } else {
                    hasEMVStartAgain!!.value = true
                    errorMessage!!.value = "Please Swipe"
                    jsonData!!.value.put("operation", "fall_back")
                    countTryAgain!!.value = 0
                }

            } else {
                hasError!!.value = true
                if (errorMessage!!.value == "") {
                    errorMessage!!.value = EMVInfoUtil.getErrorMessage(result)
                }
                hasEMVStartAgain!!.value = false
            }

        } else { // not error
            if (jsonData!!.value.optString("process") == "partialEMV") {

                getCommonData(transData, jsonData)

                if (transData.flowType.toString() == "50" || transData.flowType.toString() == "52"
                    || transData.flowType.toString() == "65"
                ) {
                    getDataMagStripe(jsonData)
                } else {
                    getDataPatialEMV(jsonData)
                }
            }
            checkEMVProcess!!.value = true
        }

    }

    fun doSendOut(ins: Int, data: ByteArray) {
        when (ins) {
            KernelINS.DISPLAY ->            // DisplayMsg: MsgID（1 byte） + Currency（1 byte）+ DataLen（1 byte） + Data（30 bytes）
                if (data[0] == MessageID.ICC_ACCOUNT.toByte()) {
                    val len = data[2].toInt()
                    val account = BytesUtil.subBytes(data, 1 + 1 + 1, len)
                    val accTLVList = TLVList.fromBinary(account)
                    var track2 = BytesUtil.bytes2HexString(accTLVList.getTLV("57").bytesValue)
                    if (track2.length == 38) {
                        track2 = track2.substring(0, track2.length - 1)
                    }
                    jsonData!!.value.put("track2", track2.toString())
                }
            KernelINS.DBLOG -> {
                var i = data.size - 1
                while (i >= 0) {
                    if (data[i].toInt() == 0x00) {
                        data[i] = 0x20
                    }
                    i--
                }
                Log.d("DBLOG", String(data))
            }
            KernelINS.CLOSE_RF ->            //outputText("=> onSendOut: Notify the application to halt contactless module");
                halt()
            else -> {}
        }
    }

    private fun outputResult(ret: Int, stepName: String?) {
        when (ret) {
            EMVError.SUCCESS -> {
            }
            EMVError.REQUEST_EXCEPTION -> {
            }
            EMVError.SERVICE_CRASH -> {
            }
            240 -> {
                hasError!!.value = true
                errorMessage!!.value = "final select function fail, please check tag"
            }
            else -> {
            }
        }
    }

    open fun getPinPublicKey(from: CAPublicKey?): PinPublicKey? {
        if (from == null) {
            return null
        }
        val to = PinPublicKey()
        to.mRid = from.rid
        to.mExp = from.exp
        to.mExpiredDate = from.expDate
        to.mHash = from.hash
        to.mHasHash = from.hashFlag
        to.mIndex = from.index
        to.mMod = from.mod
        return to
    }

}

