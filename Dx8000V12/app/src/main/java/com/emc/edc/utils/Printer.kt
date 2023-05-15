package com.emc.edc.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import com.emc.edc.emv.DeviceHelper
import com.emc.edc.getDataSetting
import com.emc.edc.getMerchantData
import com.emc.edc.globaldata.dataclass.ISO8583
import com.usdk.apiservice.aidl.printer.*
import kotlinx.coroutines.delay
import com.usdk.apiservice.aidl.vectorprinter.Alignment
import com.usdk.apiservice.aidl.vectorprinter.TextSize
import com.usdk.apiservice.aidl.vectorprinter.UVectorPrinter
import com.usdk.apiservice.aidl.vectorprinter.VectorPrinterData
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.time.format.DateTimeFormatter

class Printer(private var hasError: MutableState<Boolean>? = null,
              private var errorMessage: MutableState<String>? = null) {
    private val printer: UPrinter = DeviceHelper.me().printer
    private val vectorPrinter: UVectorPrinter? = DeviceHelper.me().vectorPrinter
    private val utils = Utils()

    fun stringToArrayHexString(data: String): ArrayList<String> {
        val isoToArray: ArrayList<String> = ArrayList()
        for (i in 0 until data.length - 1 step 2) {
            isoToArray.add(
                (data[i]).toString() + (data[i + 1]).toString()
            )
        }
        return isoToArray
    }

    fun printSaleSlip(data: JSONObject, context: Context, copyFor: String) {
        try {
            val merchantData = getMerchantData()
            val checkOperationType =
                when {
                    data.getString("operation") == "contact" -> {
                        "C"
                    }
                    data.getString("operation") == "magnetic" -> {
                        "S"
                    }
                    data.getString("operation") == "key_in" -> {
                        "K"
                    }
                    data.getString("operation") == "contactless" -> {
                        "CTLS"
                    }
                    else -> {
                        "U"
                    }
                }

            val nameSignature = data.getString("name").uppercase().split(" ")
            val logo = readAssetsFile(context, "logo_emerchant.bmp")
            printer.setPrintFormat(PrintFormat.FORMAT_ZEROSPECSET,
                PrintFormat.VALUE_FONTMODE_BOLDLEVEL2)
            printer.addBmpImage(0, FactorMode.BMP1X1, logo)


            printer.setPrnGray(10)




            printer.setPrintFormat(PrintFormat.FORMAT_MOREDATAPROC,PrintFormat.VALUE_FONTMODE_BOLDLEVEL2)

            // printer.setAscScale(ASCScale.SC2x1)
            // printer.setAscSize(ASCSize.DOT24x8)
            printer.setYSpace(5)
            printer.addText(AlignMode.CENTER, merchantData.getString("merchant_name"))
            printer.addText(AlignMode.CENTER, merchantData.getString("merchant_location"))
            printer.addText(AlignMode.CENTER, merchantData.getString("merchant_convince"))


            printer.addText(AlignMode.LEFT, "_______________________________________")
            printer.feedLine(1)
            printer.setPrnGray(3)

            ///tid
            val datatid: MutableList<Bundle> = ArrayList()
            val tid = Bundle()

            tid.putString(PrinterData.TEXT, " TID:")
            tid.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)

            datatid.add(tid)
            val value = Bundle()
            value.putString(PrinterData.TEXT, data.getString("tid"))
            value.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            datatid.add(value)
            printer.addMixStyleText(datatid)

///mid
            val datamid: MutableList<Bundle> = ArrayList()
            tid.putString(PrinterData.TEXT, " MID:")
            tid.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            datamid.add(tid)
            value.putString(PrinterData.TEXT, data.getString("mid"))
            value.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            datamid.add(value)
            printer.addMixStyleText(datamid)
            val dataTrace: MutableList<Bundle> = ArrayList()
            tid.putString(PrinterData.TEXT, " TRACE:")
            tid.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            dataTrace.add(tid)
            value.putString(PrinterData.TEXT, data.getString("invoice").padStart(6, '0').uppercase())
            value.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            dataTrace.add(value)
            printer.addMixStyleText(dataTrace)


            val datastan: MutableList<Bundle> = ArrayList()
            tid.putString(PrinterData.TEXT, " STAN:")
            tid.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            datastan.add(tid)
            value.putString(PrinterData.TEXT, data.getString("stan").padStart(6, '0').uppercase())
            value.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            datastan.add(value)
            printer.addMixStyleText(datastan)

            val databatch: MutableList<Bundle> = ArrayList()
            tid.putString(PrinterData.TEXT, " BATCH:")
            tid.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            databatch.add(tid)
            value.putString(PrinterData.TEXT, data.getString("batch_number").padStart(6, '0').uppercase())
            value.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            databatch.add(value)
            printer.addMixStyleText(databatch)




            printer.addText(AlignMode.LEFT, "----------------------------------------------")


            val type: MutableList<Bundle> = ArrayList()
            val typeOfCard = Bundle()
            typeOfCard.putString(PrinterData.TEXT," " +  data.getString("card_scheme_type"))
            typeOfCard.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            type.add(typeOfCard)
            val typeOfOperation = Bundle()
            typeOfOperation.putString(PrinterData.TEXT,
                "${data.getString("transaction_type").uppercase()} ")
            typeOfOperation.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            typeOfOperation.putInt(PrinterData.ASC_SCALE, ASCScale.SC2x1)
            //typeOfOperation.putInt(PrinterData.ASC_SIZE, ASCSize.DOT32x12)
            type.add(typeOfOperation)
            printer.addMixStyleText(type)

            printer.addText(AlignMode.LEFT,
                " " +"${data.getString("card_number_mask").uppercase()}  /$checkOperationType")
            val cardName: MutableList<Bundle> = ArrayList()
            val cardNameValue = Bundle()
            cardNameValue.putString(PrinterData.TEXT," " + data.getString("name"))
            cardNameValue.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            cardName.add(cardNameValue)
            printer.addMixStyleText(cardName)

            val dataexp: MutableList<Bundle> = ArrayList()
            tid.putString(PrinterData.TEXT, " EXP:")
            tid.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            dataexp.add(tid)
            value.putString(PrinterData.TEXT, "XX/XX")
            value.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            dataexp.add(value)

            printer.addMixStyleText(dataexp)
            printer.feedLine(1)



            val dateTime: MutableList<Bundle> = ArrayList()
            val date = Bundle()
            date.putString(PrinterData.TEXT, " " +utils.convertToDate(data.getString("date")))
            date.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            dateTime.add(date)
            val time = Bundle()
            time.putString(PrinterData.TEXT, "${utils.convertToTime(data.getString("time"))}")
            time.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            dateTime.add(time)
            printer.addMixStyleText(dateTime)




            val dataref_number: MutableList<Bundle> = ArrayList()
            tid.putString(PrinterData.TEXT, " REF NO:")
            tid.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            dataref_number.add(tid)
            value.putString(PrinterData.TEXT, data.getString("ref_num").padStart(6, '0').uppercase())
            value.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            dataref_number.add(value)
            printer.addMixStyleText(dataref_number)

            val data_aproval: MutableList<Bundle> = ArrayList()
            tid.putString(PrinterData.TEXT, " APPROVAL CODE:")
            tid.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            data_aproval.add(tid)
            value.putString(PrinterData.TEXT, data.getString("auth_id").padStart(6, '0').uppercase())
            value.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            data_aproval.add(value)
            printer.addMixStyleText(data_aproval)



            if(data.has("app_name")){
                val datacardlabel: MutableList<Bundle> = ArrayList()
                tid.putString(PrinterData.TEXT, " APP:")
                tid.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
                datacardlabel.add(tid)
                value.putString(PrinterData.TEXT,data.getString("app_name"))
                value.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
                datacardlabel.add(value)
                printer.addMixStyleText(datacardlabel)
                printer.feedLine(1)
            }



            val amount: MutableList<Bundle> = ArrayList()
            val amountKey = Bundle()
            amountKey.putString(PrinterData.TEXT, " AMT:THB")
            amountKey.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            amountKey.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            amountKey.putInt(PrinterData.ASC_SIZE, ASCSize.DOT5x7)
            amount.add(amountKey)
            val amountTotal = Bundle()
            amountTotal.putString(PrinterData.TEXT, data.getString("amount").uppercase() )
            amountTotal.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            amountTotal.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            amountTotal.putInt(PrinterData.ASC_SIZE, ASCSize.DOT5x7)
            amount.add(amountTotal)
            printer.setPrintFormat(
                PrintFormat.FORMAT_FONTMODE,
                PrintFormat.VALUE_FONTMODE_BOLDLEVEL2
            )
            printer.addMixStyleText(amount)
            printer.setPrintFormat(PrintFormat.FORMAT_FONTMODE, PrintFormat.VALUE_FONTMODE_DEFAULT)


            val tipAmount: MutableList<Bundle> = ArrayList()
            val tipAmountKey = Bundle()
            tipAmountKey.putString(PrinterData.TEXT, " TIP")
            tipAmountKey.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            tipAmountKey.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            tipAmountKey.putInt(PrinterData.ASC_SIZE, ASCSize.DOT5x7)
            tipAmount.add(tipAmountKey)
            val tipAmountTotal = Bundle()
            tipAmountTotal.putString(PrinterData.TEXT, "____________ ")
            tipAmountTotal.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            tipAmountTotal.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            tipAmount.add(tipAmountTotal)

            printer.addMixStyleText(tipAmount)

            val totalAmount: MutableList<Bundle> = ArrayList()
            val totalAmountKey = Bundle()
            totalAmountKey.putString(PrinterData.TEXT, " TOTAL")
            totalAmountKey.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            totalAmountKey.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            totalAmountKey.putInt(PrinterData.ASC_SIZE, ASCSize.DOT5x7)
            totalAmount.add(totalAmountKey)
            val totalAmountTotal = Bundle()
            totalAmountTotal.putString(PrinterData.TEXT, "____________ ")
            totalAmountTotal.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            tipAmountTotal.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            totalAmount.add(tipAmountTotal)

            printer.addMixStyleText(totalAmount)


            printer.addText(AlignMode.LEFT, "----------------------------------------------")
            printer.feedLine(1)
            if (copyFor == "merchant") {
                printer.setAscScale(ASCScale.SC1x1)
                printer.setAscSize(ASCSize.DOT5x7)
                printer.addText(AlignMode.LEFT, " SIGN X _ _ _ _ _ _ _ _ _ _ _ _ ")



                printer.setAscScale(0)
                printer.setAscSize(0)
                printer.addText(AlignMode.LEFT, "----------------------------------------------")

                printer.setAscScale(ASCScale.SC1x1)
                printer.setAscSize(ASCSize.DOT5x7)
                printer.addText(AlignMode.CENTER, "** MERCHANT COPY **")
            } else {
                printer.setAscScale(ASCScale.SC1x1)
                printer.setAscSize(ASCSize.DOT5x7)
                printer.addText(AlignMode.CENTER, "** CUSTOMER COPY **")
            }


            printer.setAscScale(ASCScale.SC1x1)
            printer.setAscSize(ASCSize.DOT24x8)
            printer.addText(AlignMode.CENTER, "I ACKNOWLEDGE SATISFACTORY RECEIPT")
            printer.addText(AlignMode.CENTER, "OF RELATIVE GOODS/SERVICES")

            printer.setPrintFormat(0, 0)
            printer.setAscScale(0)
            printer.setAscSize(0)

            printer.setAscScale(ASCScale.SC1x1)
            printer.setAscSize(ASCSize.DOT5x7)
            printer.addText(AlignMode.CENTER, "*** NO REFUND ***")

            printer.setPrintFormat(0, 0)
            printer.setAscScale(0)
            printer.setAscSize(0)


            printer.feedLine(5)



            printer.startPrint(object :
                OnPrintListener.Stub() {
                @Throws(RemoteException::class)
                override fun onFinish() {

                }


                @Throws(RemoteException::class)
                override fun onError(p0: Int) {
                    hasError!!.value = true
                    errorMessage!!.value = " error printer is ... $p0"
                    Log.e("err", "printer is ... $p0")
                }
            })
        }
        catch (e:Exception){
            hasError!!.value = true
            Log.v("TEST", "print error : $e")
            val stackTraceElements = e.stackTrace
            if (stackTraceElements.isNotEmpty()) {
                val firstStackTraceElement = stackTraceElements[0]
                val functionName = firstStackTraceElement.methodName
                val lineNumber = firstStackTraceElement.lineNumber
                errorMessage!!.value =
                    "Exception occurred in function $functionName at line $lineNumber is $e"
            }
            else{ errorMessage!!.value = "Build ISO8583 Function Error : $e"
            }
        }
    }

    private fun printSlip(curSheetNo: Int) {
        if (curSheetNo > 1) {
            return
        }

        vectorPrinter!!.startPrint(object :
            com.usdk.apiservice.aidl.vectorprinter.OnPrintListener.Stub() {
            @Throws(RemoteException::class)
            override fun onFinish() {

            }

            @Throws(RemoteException::class)
            override fun onStart() {

            }

            @Throws(RemoteException::class)
            override fun onError(error: Int, errorMsg: String) {

            }
        })

    }

    private fun readAssetsFile(ctx: Context, fileName: String): ByteArray? {
        var input: InputStream? = null
        return try {
            input = ctx.assets.open(fileName)
            val buffer = ByteArray(input.available())
            input.read(buffer)
            Log.d("Buffer", buffer.toString())
            buffer
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun printBitmap(
        data: ArrayList<String>,
        type: String,
        host: String,
    ) {
        if (getDataSetting()?.enable_print_test == true) {
            ISO8583Extracting().getISO8583Extract(data).let {
                val json = JSONObject(it)
                val jsonArray = json.getJSONArray("data")
                Log.d("Extract", it)
                printer.setPrintFormat(
                    PrintFormat.FORMAT_MOREDATAPROC,
                    PrintFormat.VALUE_MOREDATAPROC_PRNTOEND
                );
                val time = Utils().getDateTime(System.currentTimeMillis().toString())
                printer.addText(AlignMode.LEFT, "[Time] $time");
                printer.addText(AlignMode.LEFT, "[$type]");
                printer.addText(AlignMode.LEFT, "[Host] $host");
                printer.addText(AlignMode.LEFT, "[TPDU] " + json.optString("tpdu"));
                printer.addText(AlignMode.LEFT, "[Message Type] " + json.optString("messageType"));
                printer.addText(AlignMode.LEFT, "[Bitmap] " + json.optString("bitmap"));

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val bit = if (jsonObject.optString("id").length == 1) {
                        "0" + jsonObject.optString("id")
                    } else {
                        jsonObject.optString("id")
                    }
                    printer.addText(
                        AlignMode.LEFT,
                        "[DE" + bit + "] " + jsonObject.optString("originalData")
                    );
                    Log.d("Bit", "[DE" + bit + "] " + jsonObject.optString("originalData"))
                }
            }

            printer.feedLine(5)

            printer.startPrint(object : OnPrintListener.Stub() {
                @Throws(RemoteException::class)
                override fun onFinish() {
                    Log.d("Printing", "Finish")
                }

                @Throws(RemoteException::class)
                override fun onError(error: Int) {
                }
            })

            delay(1500)
        }
    }
}

//printer.setAscScale(ASCScale.SC1x1)
//printer.setAscSize(ASCSize.DOT5x7)
//printer.addText(AlignMode.CENTER, "SC1x1 DOT5x7")
//
//printer.setAscScale(ASCScale.SC1x2)
//printer.setAscSize(ASCSize.DOT5x7)
//printer.addText(AlignMode.CENTER, "SC1x2 DOT5x7")
//
//printer.setAscScale(ASCScale.SC1x3)
//printer.setAscSize(ASCSize.DOT5x7)
//printer.addText(AlignMode.CENTER, "SC1x3 DOT5x7")
//
//printer.setAscScale(ASCScale.SC2x1)
//printer.setAscSize(ASCSize.DOT5x7)
//printer.addText(AlignMode.CENTER, "SC2x1 DOT5x7")
//
//printer.setAscScale(ASCScale.SC2x2)
//printer.setAscSize(ASCSize.DOT5x7)
//printer.addText(AlignMode.CENTER, "SC2x2 DOT5x7")
//
//printer.setAscScale(ASCScale.SC2x3)
//printer.setAscSize(ASCSize.DOT5x7)
//printer.addText(AlignMode.CENTER, "SC2x3 DOT5x7")
//
//printer.setAscScale(ASCScale.SC3x1)
//printer.setAscSize(ASCSize.DOT5x7)
//printer.addText(AlignMode.CENTER, "SC3x1 DOT5x7")
//
//printer.setAscScale(ASCScale.SC3x2)
//printer.setAscSize(ASCSize.DOT5x7)
//printer.addText(AlignMode.CENTER, "SC3x2 DOT5x7")
//
//printer.setAscScale(ASCScale.SC3x3)
//printer.setAscSize(ASCSize.DOT5x7)
//printer.addText(AlignMode.CENTER, "SC3x3 DOT5x7")
////  --------------------------------
//
//printer.setAscScale(ASCScale.SC1x1)
//printer.setAscSize(ASCSize.DOT7x7)
//printer.addText(AlignMode.CENTER, "SC1x1 DOT7x7")
//
//printer.setAscScale(ASCScale.SC1x2)
//printer.setAscSize(ASCSize.DOT7x7)
//printer.addText(AlignMode.CENTER, "SC1x2 DOT7x7")
//
//printer.setAscScale(ASCScale.SC1x3)
//printer.setAscSize(ASCSize.DOT7x7)
//printer.addText(AlignMode.CENTER, "SC1x3 DOT7x7")
//
//printer.setAscScale(ASCScale.SC2x1)
//printer.setAscSize(ASCSize.DOT7x7)
//printer.addText(AlignMode.CENTER, "SC2x1 DOT7x7")
//
//printer.setAscScale(ASCScale.SC2x2)
//printer.setAscSize(ASCSize.DOT7x7)
//printer.addText(AlignMode.CENTER, "SC2x2 DOT7x7")
//
//printer.setAscScale(ASCScale.SC2x3)
//printer.setAscSize(ASCSize.DOT7x7)
//printer.addText(AlignMode.CENTER, "SC2x3 DOT7x7")
//
//printer.setAscScale(ASCScale.SC3x1)
//printer.setAscSize(ASCSize.DOT7x7)
//printer.addText(AlignMode.CENTER, "SC3x1 DOT7x7")
//
//printer.setAscScale(ASCScale.SC3x2)
//printer.setAscSize(ASCSize.DOT7x7)
//printer.addText(AlignMode.CENTER, "SC3x2 DOT7x7")
//
//printer.setAscScale(ASCScale.SC3x3)
//printer.setAscSize(ASCSize.DOT7x7)
//printer.addText(AlignMode.CENTER, "SC3x3 DOT7x7")
//
////  --------------------------------
//
//printer.setAscScale(ASCScale.SC1x1)
//printer.setAscSize(ASCSize.DOT16x8)
//printer.addText(AlignMode.CENTER, "SC1x1 DOT16x8")
//
//printer.setAscScale(ASCScale.SC1x2)
//printer.setAscSize(ASCSize.DOT16x8)
//printer.addText(AlignMode.CENTER, "SC1x2 DOT16x8")
//
//printer.setAscScale(ASCScale.SC1x3)
//printer.setAscSize(ASCSize.DOT16x8)
//printer.addText(AlignMode.CENTER, "SC1x3 DOT16x8")
//
//printer.setAscScale(ASCScale.SC2x1)
//printer.setAscSize(ASCSize.DOT16x8)
//printer.addText(AlignMode.CENTER, "SC2x1 DOT16x8")
//
//printer.setAscScale(ASCScale.SC2x2)
//printer.setAscSize(ASCSize.DOT16x8)
//printer.addText(AlignMode.CENTER, "SC2x2 DOT16x8")
//
//printer.setAscScale(ASCScale.SC2x3)
//printer.setAscSize(ASCSize.DOT16x8)
//printer.addText(AlignMode.CENTER, "SC2x3 DOT16x8")
//
//printer.setAscScale(ASCScale.SC3x1)
//printer.setAscSize(ASCSize.DOT16x8)
//printer.addText(AlignMode.CENTER, "SC3x1 DOT16x8")
//
//printer.setAscScale(ASCScale.SC3x2)
//printer.setAscSize(ASCSize.DOT16x8)
//printer.addText(AlignMode.CENTER, "SC3x2 DOT16x8")
//
//printer.setAscScale(ASCScale.SC3x3)
//printer.setAscSize(ASCSize.DOT16x8)
//printer.addText(AlignMode.CENTER, "SC3x3 DOT16x8")
//
////  --------------------------------
//
//printer.setAscScale(ASCScale.SC1x1)
//printer.setAscSize(ASCSize.DOT24x8)
//printer.addText(AlignMode.CENTER, "SC1x1 DOT24x8")
//
//printer.setAscScale(ASCScale.SC1x2)
//printer.setAscSize(ASCSize.DOT24x8)
//printer.addText(AlignMode.CENTER, "SC1x2 DOT24x8")
//
//printer.setAscScale(ASCScale.SC1x3)
//printer.setAscSize(ASCSize.DOT24x8)
//printer.addText(AlignMode.CENTER, "SC1x3 DOT24x8")
//
//printer.setAscScale(ASCScale.SC2x1)
//printer.setAscSize(ASCSize.DOT24x8)
//printer.addText(AlignMode.CENTER, "SC2x1 DOT24x8")
//
//printer.setAscScale(ASCScale.SC2x2)
//printer.setAscSize(ASCSize.DOT24x8)
//printer.addText(AlignMode.CENTER, "SC2x2 DOT24x8")
//
//printer.setAscScale(ASCScale.SC2x3)
//printer.setAscSize(ASCSize.DOT24x8)
//printer.addText(AlignMode.CENTER, "SC2x3 DOT24x8")
//
//printer.setAscScale(ASCScale.SC3x1)
//printer.setAscSize(ASCSize.DOT24x8)
//printer.addText(AlignMode.CENTER, "SC3x1 DOT24x8")
//
//printer.setAscScale(ASCScale.SC3x2)
//printer.setAscSize(ASCSize.DOT24x8)
//printer.addText(AlignMode.CENTER, "SC3x2 DOT24x8")
//
//printer.setAscScale(ASCScale.SC3x3)
//printer.setAscSize(ASCSize.DOT24x8)
//printer.addText(AlignMode.CENTER, "SC3x3 DOT24x8")
//
////  --------------------------------
//
//printer.setAscScale(ASCScale.SC1x1)
//printer.setAscSize(ASCSize.DOT24x12)
//printer.addText(AlignMode.CENTER, "SC1x1 DOT24x12")
//
//printer.setAscScale(ASCScale.SC1x2)
//printer.setAscSize(ASCSize.DOT24x12)
//printer.addText(AlignMode.CENTER, "SC1x2 DOT24x12")
//
//printer.setAscScale(ASCScale.SC1x3)
//printer.setAscSize(ASCSize.DOT24x12)
//printer.addText(AlignMode.CENTER, "SC1x3 DOT24x12")
//
//printer.setAscScale(ASCScale.SC2x1)
//printer.setAscSize(ASCSize.DOT24x12)
//printer.addText(AlignMode.CENTER, "SC2x1 DOT24x12")
//
//printer.setAscScale(ASCScale.SC2x2)
//printer.setAscSize(ASCSize.DOT24x12)
//printer.addText(AlignMode.CENTER, "SC2x2 DOT24x12")
//
//printer.setAscScale(ASCScale.SC2x3)
//printer.setAscSize(ASCSize.DOT24x12)
//printer.addText(AlignMode.CENTER, "SC2x3 DOT24x12")
//
//printer.setAscScale(ASCScale.SC3x1)
//printer.setAscSize(ASCSize.DOT24x12)
//printer.addText(AlignMode.CENTER, "SC3x1 DOT24x12")
//
//printer.setAscScale(ASCScale.SC3x2)
//printer.setAscSize(ASCSize.DOT24x12)
//printer.addText(AlignMode.CENTER, "SC3x2 DOT24x12")
//
//printer.setAscScale(ASCScale.SC3x3)
//printer.setAscSize(ASCSize.DOT24x12)
//printer.addText(AlignMode.CENTER, "SC3x3 DOT24x12")
//
////  --------------------------------
//
//printer.setAscScale(ASCScale.SC1x1)
//printer.setAscSize(ASCSize.DOT32x12)
//printer.addText(AlignMode.CENTER, "SC1x1 DOT32x12")
//
//printer.setAscScale(ASCScale.SC1x2)
//printer.setAscSize(ASCSize.DOT32x12)
//printer.addText(AlignMode.CENTER, "SC1x2 DOT32x12")
//
//printer.setAscScale(ASCScale.SC1x3)
//printer.setAscSize(ASCSize.DOT32x12)
//printer.addText(AlignMode.CENTER, "SC1x3 DOT32x12")
//
//printer.setAscScale(ASCScale.SC2x1)
//printer.setAscSize(ASCSize.DOT32x12)
//printer.addText(AlignMode.CENTER, "SC2x1 DOT32x12")
//
//printer.setAscScale(ASCScale.SC2x2)
//printer.setAscSize(ASCSize.DOT32x12)
//printer.addText(AlignMode.CENTER, "SC2x2 DOT32x12")
//
//printer.setAscScale(ASCScale.SC2x3)
//printer.setAscSize(ASCSize.DOT32x12)
//printer.addText(AlignMode.CENTER, "SC2x3 DOT32x12")
//
//printer.setAscScale(ASCScale.SC3x1)
//printer.setAscSize(ASCSize.DOT32x12)
//printer.addText(AlignMode.CENTER, "SC3x1 DOT32x12")
//
//printer.setAscScale(ASCScale.SC3x2)
//printer.setAscSize(ASCSize.DOT32x12)
//printer.addText(AlignMode.CENTER, "SC3x2 DOT32x12")
//
//printer.setAscScale(ASCScale.SC3x3)
//printer.setAscSize(ASCSize.DOT32x12)
//printer.addText(AlignMode.CENTER, "SC3x3 DOT32x12")