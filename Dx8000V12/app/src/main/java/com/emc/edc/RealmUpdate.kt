package com.emc.edc

import android.util.Log
import com.emc.edc.database.*
import io.realm.Realm
import io.realm.RealmList
import io.realm.internal.SyncObjectServerFacade
import io.realm.kotlin.toChangesetFlow
import io.realm.kotlin.where
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.pow
import com.emc.edc.globaldata.*

data class EvtUpdateRo(val class_name: String, val field_name: String, val evt_action: String)

fun GetListUpdateRO(textException: String): ArrayList<EvtUpdateRo> {
    val events = Regex("""Property.*\w+""").findAll(textException)
    val EvtsUpdateRO = ArrayList<EvtUpdateRo>()
    for (findText in events) {
        val evtSplit =
            Regex("""Property '(.*)\.(.*)' has been (.*)""").find(findText.value)?.groupValues
        EvtsUpdateRO.add(EvtUpdateRo(evtSplit!![1], evtSplit[2], evtSplit[3]))
    }
    return EvtsUpdateRO
}

fun GetConfigTerminalStringJson(): String {
    return "{" +
//            "trace_invoice: 1," +
            "app_version: \"0.1.0\"," +
            "app_password: \"000000\"," +
            "merchant_name: \"E-merchant (POS # NO.02)\"," +
            "merchant_location: \"Central Pinklao\"," +
            "merchant_convince: \"BANGKOK\"" +
            "}"
}

fun UpdateConfigTerminal(realm: Realm) {
    var config_terminal = realm.where<ConfigTerminalRO>().findFirst()
    val config_terminal_json = JSONObject(GetConfigTerminalStringJson())
    Log.v("TEST", "json: $config_terminal_json")
    Log.v("TEST", "config_terminal: ${config_terminal.toString()}")
    val new_config_terminal = ConfigTerminalRO(
        app_version = config_terminal_json.getString("app_version"),
        app_password = config_terminal_json.getString("app_password"),
        merchant_name = config_terminal_json.getString("merchant_name"),
        merchant_location = config_terminal_json.getString("merchant_location"),
        merchant_convince = config_terminal_json.getString("merchant_convince"),
    )
    if (config_terminal_json.has("trace_invoice")) {
        new_config_terminal.trace_invoice = config_terminal_json.getInt("trace_invoice")
    } else if (config_terminal != null) {
        new_config_terminal.trace_invoice = config_terminal.trace_invoice
    }
    realm.beginTransaction()
    realm.delete(ConfigTerminalRO::class.java)
    realm.copyToRealm(new_config_terminal)
    realm.commitTransaction()
    config_terminal = realm.where<ConfigTerminalRO>().findFirst()
    Log.v("TEST", config_terminal.toString())
}

fun GetProcessingCodeStringJson(): String {
    return "{\n" +
            "  \"type\": [\n" +
            "    {\n" +
            "      \"name\": \"a0\",\n" +
            "      \"data\": [\n" +
            "        {\n" +
            "          \"acc\": \"default_account\",\n" +
            "          \"code\": 0\n" +
            "        },\n" +
            "        {\n" +
            "          \"acc\": \"saving_account\",\n" +
            "          \"code\": 1\n" +
            "        },\n" +
            "        {\n" +
            "          \"acc\": \"checking_account\",\n" +
            "          \"code\": 2\n" +
            "        },\n" +
            "        {\n" +
            "          \"acc\": \"credit_account\",\n" +
            "          \"code\": 3\n" +
            "        },\n" +
            "        {\n" +
            "          \"acc\": \"universal_account\",\n" +
            "          \"code\": 4\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"0x\",\n" +
            "      \"data\":[\n" +
            "        {\n" +
            "          \"acc\": \"more_message_indicator\",\n" +
            "          \"code\": 0\n" +
            "        },\n" +
            "        {\n" +
            "          \"acc\": \"force_close_batch_request\",\n" +
            "          \"code\": 1\n" +
            "        },\n" +
            "        {\n" +
            "          \"acc\": \"initialize_after_transaction\",\n" +
            "          \"code\": 2\n" +
            "        },\n" +
            "        {\n" +
            "          \"acc\": \"currently_unassigned\",\n" +
            "          \"code\": 3\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}"
}

fun UpdateDatabaseProcessingCode(realm: Realm) {
    realm.beginTransaction()
    realm.delete(ProcessingCodeRO::class.java)
    realm.delete(ProcessingCodeDataRO::class.java)
    val processing_code_json = JSONObject(GetProcessingCodeStringJson())
    Log.v("TEST", "json: ${processing_code_json.getJSONArray("type")}")
    val list_processing_code_json = processing_code_json.getJSONArray("type")
    for (i in 0 until list_processing_code_json.length()) {
        val pc = list_processing_code_json.getJSONObject(i)
        Log.v("TEST", "pc: $pc")
        val list_processing_code_data_json = pc.getJSONArray("data")
        val list_processing_code_data = RealmList<ProcessingCodeDataRO>()
        for (i in 0 until list_processing_code_data_json.length()) {
            val pcd = list_processing_code_data_json.getJSONObject(i)
            Log.v("TEST", "pcd: $pcd")
            val processing_code_data =
                ProcessingCodeDataRO(
                    acc = pcd.get("acc") as String,
                    code = pcd.get("code") as Int?
                )
            list_processing_code_data.add(processing_code_data)
            Log.v(
                "TEST",
                "add pcd: {'acc':${processing_code_data.acc},'code':${processing_code_data.code}}"
            )
        }
        val processing_code = ProcessingCodeRO(
            name = pc.get("name") as String,
            data = list_processing_code_data
        )
        realm.copyToRealm(processing_code)
    }
    realm.commitTransaction()
}

fun GetTransactionStringJson(): String {
    return "" +
            "{\n" +
            "  \"transaction\": [\n" +
            "    {\n" +
            "      \"type\": \"sale\",\n" +
            "      \"processing_code\": [\"00\", \"A0\", \"0X\"],\n" +
            "      \"reversal\": true,\n" +
            "      \"msgType\": \"0200\",\n" +
            "      \"bitmap\": [\n" +
            "        {\n" + //
            "          \"type\": \"magnetic\",\n" +
            "          \"bitmaps\": [3,4,11,22,24,25,35,41,42,45,62]\n" +
            "        },\n" + //
            "        {\n" + //
            "          \"type\": \"key_in\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        },\n" + //
            "        {\n" + //
            "          \"type\": \"contact\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,55,62]\n" +
            "        },\n" + //
            "        {\n" + //
            "          \"type\": \"contactless\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,55,62]\n" +
            "        },\n" + //
            "        {\n" + //
            "          \"type\": \"mag_stripe\",\n" +
            "          \"bitmaps\": [3,4,11,22,24,25,35,41,42,45,62]\n" +
            "        }\n" + //
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"pre_auth\",\n" +
            "      \"processing_code\": [\"30\", \"A0\", \"0X\"],\n" +
            "      \"reversal\": true,\n" +
            "      \"msgType\": \"0100\",\n" +
            "      \"bitmap\": [\n" +
            "        {\n" +
            "          \"type\": \"magnetic\",\n" +
            "          \"bitmaps\": [3,4,11,22,24,25,35,41,42,45,62]\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"key_in\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        },\n" +
            "           {\n" + //
            "          \"type\": \"contact\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        },\n" + //
            "        {\n" + //
            "          \"type\": \"contactless\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        }\n" + //
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"refund\",\n" +
            "      \"processing_code\": [\"20\", \"A0\", \"0X\"],\n" +
            "      \"reversal\": true,\n" +
            "      \"msgType\": \"0200\",\n" +
            "      \"bitmap\": [\n" +
            "        {\n" +
            "          \"type\": \"magnetic\",\n" +
            "          \"bitmaps\": [3,4,11,22,24,25,35,41,42,45,62]\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"key_in\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        },\n" +
            "           {\n" + //
            "          \"type\": \"contact\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        },\n" + //
            "        {\n" + //
            "          \"type\": \"contactless\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        }\n" + //
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"offline_sale\",\n" +
            "      \"processing_code\": [\"00\", \"A0\", \"0X\"],\n" +
            "      \"reversal\": false,\n" +
            "      \"msgType\": \"0220\",\n" +
            "      \"bitmap\": [\n" +
            "        {\n" +
            "          \"type\": \"magnetic\",\n" +
            "          \"bitmaps\": [2,3,4,11,12,13,14,22,24,25,38,41,42,62]\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"key_in\",\n" +
            "          \"bitmaps\": [2,3,4,11,12,13,14,22,24,25,38,41,42,62]\n" +
            "        },\n" +
            "           {\n" + //
            "          \"type\": \"contact\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        },\n" + //
            "        {\n" + //
            "          \"type\": \"contactless\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        }\n" + //
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"test_host\",\n" +
            "      \"processing_code\": [\"99\", \"00\", \"0X\"],\n" +
            "      \"reversal\": false,\n" +
            "      \"msgType\": \"0800\",\n" +
            "      \"bitmap\": [\n" +
            "        {\n" +
            "          \"type\": \"test_host\",\n" +
            "          \"bitmaps\": [3,24,41,42]\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"balance_inquiry\",\n" +
            "      \"processing_code\": [\"31\", \"A0\", \"00\"],\n" +
            "      \"reversal\": false,\n" +
            "      \"msgType\": \"0100\",\n" +
            "      \"bitmap\": [\n" +
            "        {\n" +
            "          \"type\": \"magnetic\",\n" +
            "          \"bitmaps\": [3,11,22,24,25,35,41,42,45]\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"key_in\",\n" +
            "          \"bitmaps\": [2,3,11,14,22,24,25,41,42]\n" +
            "        },\n" +
            "           {\n" + //
            "          \"type\": \"contact\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        },\n" + //
            "        {\n" + //
            "          \"type\": \"contactless\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        }\n" + //
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"void_sale\",\n" +
            "      \"processing_code\": [\"02\", \"A0\", \"0X\"],\n" +
            "      \"reversal\": true,\n" +
            "      \"msgType\": \"0200\",\n" +
            "      \"bitmap\": [\n" +
            "        {\n" +
            "          \"type\": \"magnetic\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,37,41,42,62]\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"key_in\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,37,41,42,62]\n" +
            "        },\n" +
            "           {\n" + //
            "          \"type\": \"contact\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        },\n" + //
            "        {\n" + //
            "          \"type\": \"contactless\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        }\n" + //
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"void_refund\",\n" +
            "      \"processing_code\": [\"22\", \"A0\", \"0X\"],\n" +
            "      \"reversal\": true,\n" +
            "      \"msgType\": \"0200\",\n" +
            "      \"bitmap\": [\n" +
            "        {\n" +
            "          \"type\": \"magnetic\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,37,41,42,62]\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"key_in\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,37,41,42,62]\n" +
            "        },\n" +
            "           {\n" + //
            "          \"type\": \"contact\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        },\n" + //
            "        {\n" + //
            "          \"type\": \"contactless\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        }\n" + //
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"sale_complete\",\n" +
            "      \"processing_code\": [\"00\", \"A0\", \"0X\"],\n" +
            "      \"reversal\": false,\n" +
            "      \"msgType\": \"0220\",\n" +
            "      \"bitmap\": [\n" +
            "        {\n" +
            "          \"type\": \"magnetic\",\n" +
            "          \"bitmaps\": [2,3,4,11,12,13,14,22,24,25,37,38,39,41,42,54,62]\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"key_in\",\n" +
            "          \"bitmaps\": [2,3,4,11,12,13,14,22,24,25,37,38,39,41,42,54,62]\n" +
            "        },\n" +
            "           {\n" + //
            "          \"type\": \"contact\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        },\n" + //
            "        {\n" + //
            "          \"type\": \"contactless\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        }\n" + //
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"tip_adjust_sale\",\n" +
            "      \"processing_code\": [\"02\", \"A0\", \"0X\"],\n" +
            "      \"reversal\": false,\n" +
            "      \"msgType\": \"0220\",\n" +
            "      \"bitmap\": [\n" +
            "        {\n" +
            "          \"type\": \"magnetic\",\n" +
            "          \"bitmaps\": [2,3,4,11,12,13,14,22,24,25,37,38,41,42,54,60,62]\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"key_in\",\n" +
            "          \"bitmaps\": [2,3,4,11,12,13,14,22,24,25,37,38,41,42,54,60,62]\n" +
            "        },\n" +
            "           {\n" + //
            "          \"type\": \"contact\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        },\n" + //
            "        {\n" + //
            "          \"type\": \"contactless\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        }\n" + //
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"tip_adjust_refund\",\n" +
            "      \"processing_code\": [\"22\", \"A0\", \"0X\"],\n" +
            "      \"reversal\": false,\n" +
            "      \"msgType\": \"0220\",\n" +
            "      \"bitmap\": [\n" +
            "        {\n" +
            "          \"type\": \"magnetic\",\n" +
            "          \"bitmaps\": [2,3,4,11,12,13,14,22,24,25,37,38,39,41,42,54,62]\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"key_in\",\n" +
            "          \"bitmaps\": [2,3,4,11,12,13,14,22,24,25,37,38,39,41,42,54,62]\n" +
            "        },\n" +
            "           {\n" + //
            "          \"type\": \"contact\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        },\n" + //
            "        {\n" + //
            "          \"type\": \"contactless\",\n" +
            "          \"bitmaps\": [2,3,4,11,14,22,24,25,41,42,62]\n" +
            "        }\n" + //
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}"
}

fun UpdateDatabaseTransaction(realm: Realm) {
    realm.beginTransaction()
    realm.delete(ConfigTransactionRO::class.java)
    realm.delete(DataBitmapRO::class.java)
    val config_transactions = JSONObject(GetTransactionStringJson())
    Log.v("TEST", "json: ${config_transactions.getJSONArray("transaction")}")
    val list_transaction_json = config_transactions.getJSONArray("transaction")
    for (i in 0 until list_transaction_json.length()) {
        val tst = list_transaction_json.getJSONObject(i)
        Log.v("TEST", "tst: $tst")
        val list_transaction_bitmap_json = tst.getJSONArray("bitmap")
        val list_transaction_bitmap = RealmList<DataBitmapRO>()
        for (i in 0 until list_transaction_bitmap_json.length()) {
            val tst_bm = list_transaction_bitmap_json.getJSONObject(i)
            Log.v("TEST", "tst_bm: $tst_bm")
            val bitmaps_realm = RealmList<Int>()
            val bitmaps = tst_bm.get("bitmaps") as JSONArray
            for (i in 0 until bitmaps.length()) {
                bitmaps_realm.add(bitmaps[i] as Int?)
            }
            val transaction_bitmap = DataBitmapRO(
                type = tst_bm.get("type") as String,
                bitmaps = bitmaps_realm
            )
            list_transaction_bitmap.add(transaction_bitmap)
            Log.v(
                "TEST",
                "add tst_bm: {'type':${transaction_bitmap.type},'bitmaps':${transaction_bitmap.bitmaps}}"
            )
        }
        val processing_code_realm = RealmList<String>()
        val processing_code = tst.get("processing_code") as JSONArray
        for (i in 0 until processing_code.length()) {
            processing_code_realm.add(processing_code[i] as String?)
        }
        val config_transaction = ConfigTransactionRO(
            type = tst.get("type") as String,
            processing_code = processing_code_realm,
            reversal = tst.get("reversal") as Boolean,
            msgType = tst.get("msgType") as String,
            bitmap = list_transaction_bitmap,
        )
        realm.copyToRealm(config_transaction)
    }
    val transaction_type = "sale"
    val data: ConfigTransactionRO? =
        realm.where<ConfigTransactionRO>().equalTo("type", transaction_type).findFirst()
    Log.v("TEST", data.toString())
    realm.commitTransaction()
}


fun GetConfigCardStringJson(): String {
    return "{" +
            "config_card: [" +
            "{" +
            "card_record_index: 1,\n" +
            "low_prefix: \"478448\",\n" +
            "high_prefix: \"478448\",\n" +
            "card_label: \"VISA PLATINUM\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 1,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"credit\",\n" +
            "card_scheme_type: \"visa card\"" +
            "}," +
            "{" +
            "card_record_index: 2,\n" +
            "low_prefix: \"483000\",\n" +
            "high_prefix: \"483099\",\n" +
            "card_label: \"SCB CARD\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 1,\n" +
            "host_record_index: 2,\n" +
            "credit_debit_type: \"debit\",\n" +
            "card_scheme_type: \"visa card\"" +
            "}," +
            "{" +
            "card_record_index: 3,\n" +
            "low_prefix: \"5501300\",\n" +
            "high_prefix: \"5501300\",\n" +
            "card_label: \"KBANK-PLATINUM\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 2,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"debit\",\n" +
            "card_scheme_type: \"mastercard\"" +
            "}," +
            "{" +
            "card_record_index: 4,\n" +
            "low_prefix: \"478867\",\n" +
            "high_prefix: \"478867\",\n" +
            "card_label: \"SCB-CARD\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 1,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"debit\",\n" +
            "card_scheme_type: \"visa card\"" +
            "}," +
            "{" +
            "card_record_index: 5,\n" +
            "low_prefix: \"478445\",\n" +
            "high_prefix: \"478445\",\n" +
            "card_label: \"TMRW\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 2,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"debit\",\n" +
            "card_scheme_type: \"mastercard\"" +
            "}," +
            "{" +
            "card_record_index: 6,\n" +
            "low_prefix: \"422243\",\n" +
            "high_prefix: \"422243\",\n" +
            "card_label: \"CITI\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 2,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"debit\",\n" +
            "card_scheme_type: \"visa card\"" +
            "}," +
            "{" +
            "card_record_index: 7,\n" +
            "low_prefix: \"000410\",\n" +
            "high_prefix: \"000410\",\n" +
            "card_label: \"KBANK\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 2,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"credit\",\n" +
            "card_scheme_type: \"visa card\"" +
            "}," +
            "{" +
            "card_record_index: 8,\n" +
            "low_prefix: \"455205\",\n" +
            "high_prefix: \"455205\",\n" +
            "card_label: \"KRUNGSRI\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 2,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"credit\",\n" +
            "card_scheme_type: \"visa card\"" +
            "}," +
            "{" +
            "card_record_index: 9,\n" +
            "low_prefix: \"441770\",\n" +
            "high_prefix: \"441770\",\n" +
            "card_label: \"KBank\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 2,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"credit\",\n" +
            "card_scheme_type: \"visa card\"" +
            "}," +
            "{" +
            "card_record_index: 10,\n" +
            "low_prefix: \"532306\",\n" +
            "high_prefix: \"532306\",\n" +
            "card_label: \"KBank\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 2,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"credit\",\n" +
            "card_scheme_type: \"mastercard\"" +
            "}," +
            "{" +
            "card_record_index: 12,\n" +
            "low_prefix: \"356999\",\n" +
            "high_prefix: \"356999\",\n" +
            "card_label: \"KBank\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 2,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"credit\",\n" +
            "card_scheme_type: \"JCB/TEST\"" +
            "}," +
            "{" +
            "card_record_index: 13,\n" +
            "low_prefix: \"623355\",\n" +
            "high_prefix: \"623355\",\n" +
            "card_label: \"KBank\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 2,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"debit\",\n" +
            "card_scheme_type: \"UnionPay\"" +
            "}," +
            "{" +
            "card_record_index: 14,\n" +
            "low_prefix: \"476173\",\n" +
            "high_prefix: \"476173\",\n" +
            "card_label: \"KBank\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 2,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"credit\",\n" +
            "card_scheme_type: \"JCB ULtool\"" +
            "}," +
            "{" +
            "card_record_index: 15,\n" +
            "low_prefix: \"541333\",\n" +
            "high_prefix: \"541333\",\n" +
            "card_label: \"KBank\",\n" +
            "pan_min_length: 16,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 2,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"credit\",\n" +
            "card_scheme_type: \"Master Card ULtool\"" +
            "}," +
            "{" +
            "card_record_index: 16,\n" +
            "low_prefix: \"374245\",\n" +
            "high_prefix: \"374245\",\n" +
            "card_label: \"KBank\",\n" +
            "pan_min_length: 15,\n" +
            "pan_max_length: 19,\n" +
            "card_control_record_index: 3,\n" +
            "host_record_index: 1,\n" +
            "credit_debit_type: \"credit\",\n" +
            "card_scheme_type: \"AMEX ULtool\"" +
            "}" +
            "]" +
            "}"
}
//356999
/*
fun GetConfigCardStringJson() : String{
    return  "{" +
                "config_card: [" +
                   "{" +
                        "card_record_index: 1,\n" +
                        "low_prefix: \"478448\",\n" +
                        "high_prefix: \"478448\",\n" +
                        "card_label: \"VISA PLATINUM\",\n" +
                        "pan_min_length: 16,\n" +
                        "pan_max_length: 19,\n" +
                        "card_control_record_index: 1,\n" +
                        "host_record_index: 1,\n" +
                        "credit_debit_type: \"credit\",\n" +
                        "card_scheme_type: \"VISA CARD\"" +
                    "}," +
                    "{" +
                        "card_record_index: 2,\n" +
                        "low_prefix: \"483000\",\n" +
                        "high_prefix: \"483099\",\n" +
                        "card_label: \"SCB CARD\",\n" +
                        "pan_min_length: 16,\n" +
                        "pan_max_length: 19,\n" +
                        "card_control_record_index: 1,\n" +
                        "host_record_index: 2,\n" +
                        "credit_debit_type: \"debit\",\n" +
                        "card_scheme_type: \"VISA CARD\"" +
                    "}," +
                    "{" +
                        "card_record_index: 3,\n" +
                        "low_prefix: \"5501300\",\n" +
                        "high_prefix: \"5501300\",\n" +
                        "card_label: \"KBANK-PLATINUM\",\n" +
                        "pan_min_length: 16,\n" +
                        "pan_max_length: 19,\n" +
                        "card_control_record_index: 2,\n" +
                        "host_record_index: 1,\n" +
                        "credit_debit_type: \"debit\",\n" +
                        "card_scheme_type: \"mastercard\"" +
                    "}," +
                    "{" +
                        "card_record_index: 4,\n" +
                        "low_prefix: \"478867\",\n" +
                        "high_prefix: \"478867\",\n" +
                        "card_label: \"SCB-CARD\",\n" +
                        "pan_min_length: 16,\n" +
                        "pan_max_length: 19,\n" +
                        "card_control_record_index: 1,\n" +
                        "host_record_index: 1,\n" +
                        "credit_debit_type: \"debit\",\n" +
                        "card_scheme_type: \"VISA CARD\"" +
                    "}," +
                    "{" +
                        "card_record_index: 5,\n" +
                        "low_prefix: \"478\",\n" +
                        "high_prefix: \"478\",\n" +
                        "card_label: \"TMRW\",\n" +
                        "pan_min_length: 16,\n" +
                        "pan_max_length: 19,\n" +
                        "card_control_record_index: 3,\n" +
                        "host_record_index: 1,\n" +
                        "credit_debit_type: \"debit\",\n" +
                        "card_scheme_type: \"MASTERCARD\"" +
                    "}" +
                "]" +
            "}"
}
 */


fun UpdateDatabaseConfigCard(realm: Realm) {
    realm.beginTransaction()
    realm.delete(ConfigCardRO::class.java)
    realm.delete(ConfigCardRangeRO::class.java)
    val config_cards = JSONObject(GetConfigCardStringJson())
    Log.v("TEST", "json: ${config_cards.getJSONArray("config_card")}")
    val list_config_card_json = config_cards.getJSONArray("config_card")
    for (i in 0 until list_config_card_json.length()) {
        val cc = list_config_card_json.getJSONObject(i)
//        Log.v("TEST", "tst: $cc")
        val config_card = ConfigCardRO(
            card_record_index = cc.getInt("card_record_index"),
            low_prefix = cc.getString("low_prefix"),
            high_prefix = cc.getString("high_prefix"),
            card_label = cc.getString("card_label"),
            min_length = cc.getInt("pan_min_length"),
            max_length = cc.getInt("pan_max_length"),
            card_control_record_index = cc.getInt("card_control_record_index"),
            host_record_index = cc.getInt("host_record_index"),
            credit_debit_type = cc.getString("credit_debit_type"),
            card_scheme_type = cc.getString("card_scheme_type"),
        )
        Log.v("TEST", "cc: $config_card")
        realm.copyToRealm(config_card)
        val start_lenght = cc.getInt("pan_min_length")
        val stop_lenght = cc.getInt("pan_max_length")
        for (lenght in start_lenght..stop_lenght) {
            var low_range = cc.getString("low_prefix")
            var high_range = (cc.getString("high_prefix").toInt() + 1).toString()
            for (s in 1..lenght - low_range.length) {
                low_range += "0"
                high_range += "0"
            }
            val config_card_range = ConfigCardRangeRO(
                card_record_index = cc.getInt("card_record_index"),
                low_range = low_range.toLong(),
                high_range = high_range.toLong(),
                card_number_length = lenght,
            )
            Log.v("TEST", "ccr: $config_card_range")
            realm.copyToRealm(config_card_range)
        }
    }
    realm.commitTransaction()
}

fun GetConfigCardControlStringJson(): String {
    return "{" +
            "config_card_control: [" +
            "{" +
            "card_control_record_index: 1,\n" +
            "sale_allow: true,\n" +
            "sale_password: null,\n" +
            "void_allow: true,\n" +
            "void_password: null,\n" +
            "refund_allow: true,\n" +
            "refund_password: null,\n" +
            "preauth_allow: true,\n" +
            "preauth_password: null,\n" +
            "maunaul_entry_allow: true,\n" +
            "maunaul_entry_password: null,\n" +
            "sale_complete_allow: true,\n" +
            "sale_complete_password: null,\n" +
            "offline_allow: true,\n" +
            "offline_password: null,\n" +
            "settlement_allow: true,\n" +
            "settlement_password: null,\n" +
            "pan_masking: \"NNNN XXXX XXXX XXNN\"" +
            "}," +
            "{" +
            "card_control_record_index: 2,\n" +
            "sale_allow: true,\n" +
            "sale_password: null,\n" +
            "void_allow: true,\n" +
            "void_password: null,\n" +
            "refund_allow: true,\n" +
            "refund_password: null,\n" +
            "preauth_allow: true,\n" +
            "preauth_password: null,\n" +
            "maunaul_entry_allow: true,\n" +
            "maunaul_entry_password: null,\n" +
            "sale_complete_allow: true,\n" +
            "sale_complete_password: null,\n" +
            "offline_allow: true,\n" +
            "offline_password: null,\n" +
            "settlement_allow: true,\n" +
            "settlement_password: null,\n" +
            "pan_masking: \"NNNN XXXX XXXX XNNN\"" +
            "}," +
            "{" +
            "card_control_record_index: 3,\n" +
            "sale_allow: true,\n" +
            "sale_password: null,\n" +
            "void_allow: true,\n" +
            "void_password: null,\n" +
            "refund_allow: true,\n" +
            "refund_password: null,\n" +
            "preauth_allow: true,\n" +
            "preauth_password: null,\n" +
            "maunaul_entry_allow: true,\n" +
            "maunaul_entry_password: null,\n" +
            "sale_complete_allow: true,\n" +
            "sale_complete_password: null,\n" +
            "offline_allow: true,\n" +
            "offline_password: null,\n" +
            "settlement_allow: true,\n" +
            "settlement_password: null,\n" +
            "pan_masking: \"NNNN NNXXXXXX NNN\"" +
            "}" +
            "]" +
            "}"
}

fun UpdateDatabaseConfigCardControl(realm: Realm) {
    realm.beginTransaction()
    realm.delete(ConfigCardControlRO::class.java)
    val config_card_controls = JSONObject(GetConfigCardControlStringJson())
    Log.v("TEST", "json: ${config_card_controls.getJSONArray("config_card_control")}")
    val list_config_card_control_json = config_card_controls.getJSONArray("config_card_control")
    for (i in 0 until list_config_card_control_json.length()) {
        val ccc = list_config_card_control_json.getJSONObject(i)
        Log.v("TEST", "ccc: $ccc")
        val config_card_control = ConfigCardControlRO(
            card_control_record_index = ccc.getInt("card_control_record_index"),
            sale_allow = ccc.getBoolean("sale_allow"),
            sale_password = ccc.getString("sale_password"),
            void_allow = ccc.getBoolean("void_allow"),
            void_password = ccc.getString("void_password"),
            refund_allow = ccc.getBoolean("refund_allow"),
            refund_password = ccc.getString("refund_password"),
            preauth_allow = ccc.getBoolean("preauth_allow"),
            preauth_password = ccc.getString("preauth_password"),
            maunaul_entry_allow = ccc.getBoolean("maunaul_entry_allow"),
            maunaul_entry_password = ccc.getString("maunaul_entry_password"),
            sale_complete_allow = ccc.getBoolean("sale_complete_allow"),
            sale_complete_password = ccc.getString("sale_complete_password"),
            offline_allow = ccc.getBoolean("offline_allow"),
            offline_password = ccc.getString("offline_password"),
            settlement_allow = ccc.getBoolean("settlement_allow"),
            settlement_password = ccc.getString("settlement_password"),
            pan_masking = ccc.getString("pan_masking")
        )
        realm.copyToRealm(config_card_control)
    }
    realm.commitTransaction()
}

fun GetConfigHostStringJson(): String {
    return "{" +
            "config_host: [" +
            "{" +
            "host_record_index: 1,\n" +
            "host_define_type: 1,\n" +
            "stan: 1,\n" +
            "host_acquieretype: \"\",\n" +
            "host_label_name: \"Kbank\",\n" +
            //"terminal_id: \"11111111\",\n" +
            "terminal_id: \"11111111\",\n" +
            "merchant_id: \"111111111111111\",\n" +
            "nii: \"0120\",\n" +
            //                       "ip_address1: \"192.168.152.175\",\n" +
            //                "ip_address1: \"plert.xeus.dev\",\n" +
            "ip_address1: \"192.168.137.7\",\n" +
//            "ip_address1: \"192.168.40.52\",\n" +
            "ip_address2: null,\n" +
            "ip_address3: null,\n" +
            "ip_address4: null,\n" +
//                        "port1: 7500,\n" +
            "port1: 5500,\n" +
//            "port1: 2100,\n" +
            "port2: null,\n" +
            "port3: null,\n" +
            "port4: null,\n" +
//                        "reversal_flag: true,\n" +
//                        "reversal_msg: \"0042600120800004007024058000c00000164830990000183673003000000000000999000003220800220120003131313131313131313131313131313131313131313131\",\n" +
            "reversal_flag: false,\n" +
            "reversal_msg: null,\n" +
            "last_batch_number: 1,\n" +
            "logo_file: null,\n" +
            "acquire_id: 0,\n" +
            "pre_auth_max_day: 15,\n" +
            "test: null" +
            "}," +
            "{" +
            "host_record_index: 2,\n" +
            "host_define_type: 2,\n" +
            "stan: 1,\n" +
            "host_acquieretype: \"\",\n" +
            "host_label_name: \"HOST_TEST_SITTHICHAI\",\n" +
            "terminal_id: \"11111111\",\n" +
            "merchant_id: \"111111111111111\",\n" +
            "nii: \"0120\",\n" +
            //           "ip_address1: \"192.168.131.129\",\n" +
            "ip_address1: \"192.168.137.7\",\n" +
            "ip_address2: null,\n" +
            "ip_address3: null,\n" +
            "ip_address4: null,\n" +
            "port1: 5500,\n" +
            "port2: null,\n" +
            "port3: null,\n" +
            "port4: null,\n" +
            "reversal_flag: false,\n" +
            "reversal_msg: null,\n" +
            "last_batch_number: 1,\n" +
            "logo_file: null,\n" +
            "acquire_id: 0,\n" +
            "pre_auth_max_day: 15,\n" +
            "test: null" +
            "}" +
            "]" +
            "}"
}

fun UpdateDatabaseConfigHost(realm: Realm) {
//    realm.beginTransaction()
//    realm.delete(ConfigHostRO::class.java)
    val config_hosts = JSONObject(GetConfigHostStringJson())
    Log.v("TEST", "json: ${config_hosts.getJSONArray("config_host")}")
    val list_config_host_json = config_hosts.getJSONArray("config_host")

    for (i in 0 until list_config_host_json.length()) {
        val ch = list_config_host_json.getJSONObject(i)
        Log.v("TEST", "ch: $ch")
        realm.executeTransaction {
            val search_host = it.where<ConfigHostRO>()
                .equalTo("host_record_index", ch.getInt("host_record_index")).findFirst()
            if (search_host != null) {
                search_host.host_record_index =
                    if (ch.has("host_record_index")) ch.getInt("host_record_index")
                    else search_host.host_record_index
                search_host.host_label_name =
                    if (ch.has("host_label_name")) ch.getString("host_label_name")
                    else search_host.host_label_name
                search_host.host_define_type =
                    if (ch.has("host_define_type")) ch.getInt("host_define_type")
                    else search_host.host_define_type
                search_host.terminal_id =
                    if (ch.has("terminal_id")) ch.getString("terminal_id")
                    else search_host.terminal_id
                search_host.merchant_id =
                    if (ch.has("merchant_id")) ch.getString("merchant_id")
                    else search_host.merchant_id
                search_host.nii =
                    if (ch.has("nii")) ch.getString("nii") else search_host.nii
                search_host.ip_address1 =
                    if (ch.has("ip_address1")) ch.getString("ip_address1")
                    else search_host.ip_address1
                search_host.port1 =
                    if (ch.has("port1")) ch.getInt("port1")
                    else search_host.port1
                search_host.pre_auth_max_day =
                    if (ch.has("pre_auth_max_day")) ch.getInt("pre_auth_max_day")
                    else search_host.pre_auth_max_day
                ////////// only dev mode //////////
                search_host.last_batch_number =
                    if (ch.has("last_batch_number")) ch.getInt("last_batch_number")
                    else search_host.last_batch_number
                /*search_host.reversal_flag =
                    if (ch.has("reversal_flag")) ch.getBoolean("reversal_flag")
                    else search_host.reversal_flag
                search_host.reversal_msg =
                    if (ch.has("reversal_msg")) ch.getString("reversal_msg")
                    else search_host.reversal_msg*/
                ////////////////////////////////////
            } else {
                val config_host = ConfigHostRO(
                    host_record_index = ch.getInt("host_record_index"),
                    host_define_type = ch.getInt("host_define_type"),
                    terminal_id = ch.getString("terminal_id"),
                    merchant_id = ch.getString("merchant_id"),
                    nii = ch.getString("nii"),
                    ip_address1 = ch.getString("ip_address1"),
                    port1 = ch.getInt("port1"),
                    reversal_flag = ch.getBoolean("reversal_flag"),
                    reversal_msg = ch.getString("reversal_msg"),
                    pre_auth_max_day = ch.getInt("pre_auth_max_day"),
                )
                realm.copyToRealm(config_host)
            }
        }

    }
//    realm.commitTransaction()
}

fun GetConfigCapkStringJson(): String {
    return "{" +
            "config_capk: [" +
            "{" +
            "rid: \"A000000003\",\n" +
            "capk_index: \"01\",\n" +
            "mod: \"C696034213D7D8546984579D1D0F0EA519CFF8DEFFC429354CF3A871A6F7183F1228DA5C7470C055387100CB935A712C4E2864DF5D64BA93FE7E63E71F25B1E5F5298575EBE1C63AA617706917911DC2A75AC28B251C7EF40F2365912490B939BCA2124A30A28F54402C34AECA331AB67E1E79B285DD5771B5D9FF79EA630B75\",\n" +
            "exponent: \"03\",\n" +
            "issuer: \"visa\",\n" +
            "key_length: 1024,\n" +
            "sha1: \"D34A6A776011C7E7CE3AEC5F03AD2F8CFC5503CC\",\n" +
            "exp: \"20991231\"" +
            "}," +
            "{" +
            "rid: \"A000000004\",\n" +
            "capk_index: \"06\",\n" +
            "mod: \"CB26FC830B43785B2BCE37C81ED334622F9622F4C89AAE641046B2353433883F307FB7C974162DA72F7A4EC75D9D657336865B8D3023D3D645667625C9A07A6B7A137CF0C64198AE38FC238006FB2603F41F4F3BB9DA1347270F2F5D8C606E420958C5F7D50A71DE30142F70DE468889B5E3A08695B938A50FC980393A9CBCE44AD2D64F630BB33AD3F5F5FD495D31F37818C1D94071342E07F1BEC2194F6035BA5DED3936500EB82DFDA6E8AFB655B1EF3D0D7EBF86B66DD9F29F6B1D324FE8B26CE38AB2013DD13F611E7A594D675C4432350EA244CC34F3873CBA06592987A1D7E852ADC22EF5A2EE28132031E48F74037E3B34AB747F\",\n" +
            "exponent: \"03\",\n" +
            "issuer: \"master_card\",\n" +
            "key_length: 1984,\n" +
            "sha1: \"F910A1504D5FFB793D94F3B500765E1ABCAD72D9\",\n" +
            "exp: \"20281231\"" +
            "}," +
            "{" +
            "rid: \"A000000003\",\n" +
            "capk_index: \"09\",\n" +
            "mod: \"9D912248DE0A4E39C1A7DDE3F6D2588992C1A4095AFBD1824D1BA74847F2BC4926D2EFD904B4B54954CD189A54C5D1179654F8F9B0D2AB5F0357EB642FEDA95D3912C6576945FAB897E7062CAA44A4AA06B8FE6E3DBA18AF6AE3738E30429EE9BE03427C9D64F695FA8CAB4BFE376853EA34AD1D76BFCAD15908C077FFE6DC5521ECEF5D278A96E26F57359FFAEDA19434B937F1AD999DC5C41EB11935B44C18100E857F431A4A5A6BB65114F174C2D7B59FDF237D6BB1DD0916E644D709DED56481477C75D95CDD68254615F7740EC07F330AC5D67BCD75BF23D28A140826C026DBDE971A37CD3EF9B8DF644AC385010501EFC6509D7A41\",\n" +
            "exponent: \"03\",\n" +
            "issuer: \"visa\",\n" +
            "key_length: 1984,\n" +
            "sha1: \"1FF80A40173F52D7D27E0F26A146A1C8CCB29046\",\n" +
            "exp: \"20281231\"" +
            "}," +
            "{" +
            "rid: \"A000000003\",\n" +
            "capk_index: \"08\",\n" +
            "mod: \"D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0B\",\n" +
            "exponent: \"03\",\n" +
            "issuer: \"visa\",\n" +
            "key_length: 1408,\n" +
            "sha1: \"20D213126955DE205ADC2FD2822BD22DE21CF9A8\",\n" +
            "exp: \"20241231\"" +
            "}," +
            "{" +
            "rid: \"A000000065\",\n" +
            "capk_index: \"03\",\n" +
            "mod: \"ADF05CD4C5B490B087C3467B0F3043750438848461288BFEFD6198DD576DC3AD7A7CFA07DBA128C247A8EAB30DC3A30B02FCD7F1C8167965463626FEFF8AB1AA61A4B9AEF09EE12B009842A1ABA01ADB4A2B170668781EC92B60F605FD12B2B2A6F1FE734BE510F60DC5D189E401451B62B4E06851EC20EBFF4522AACC2E9CDC89BC5D8CDE5D633CFD77220FF6BBD4A9B441473CC3C6FEFC8D13E57C3DE97E1269FA19F655215B23563ED1D1860D8681\",\n" +
            "exponent: \"03\",\n" +
            "issuer: \"JCB\",\n" +
            "key_length: 1984,\n" +
            "sha1: \"CD82812F9E844048F48ED56ED84EFB3267E870F8\",\n" +
            "exp: \"20300101\"" +
            "}," +
            "{" +
            "rid: \"A000000065\",\n" +
            "capk_index: \"13\",\n" +
            "mod: \"A3270868367E6E29349FC2743EE545AC53BD3029782488997650108524FD051E3B6EACA6A9A6C1441D28889A5F46413C8F62F3645AAEB30A1521EEF41FD4F3445BFA1AB29F9AC1A74D9A16B93293296CB09162B149BAC22F88AD8F322D684D6B49A12413FC1B6AC70EDEDB18EC1585519A89B50B3D03E14063C2CA58B7C2BA7FB22799A33BCDE6AFCBEB4A7D64911D08D18C47F9BD14A9FAD8805A15DE5A38945A97919B7AB88EFA11A88C0CD92C6EE7DC352AB0746ABF13585913C8A4E04464B77909C6BD94341A8976C4769EA6C0D30A60F4EE8FA19E767B170DF4FA80312DBA61DB645D5D1560873E2674E1F620083F30180BD96CA589\",\n" +
            "exponent: \"03\",\n" +
            "issuer: \"JCB\",\n" +
            "key_length: 1984,\n" +
            "sha1: \"54CFAE617150DFA09D3F901C9123524523EBEDF3\",\n" +
            "exp: \"20300101\"" +
            "}," +
            "{" +
            "rid: \"A000000677\",\n" +
            "capk_index: \"03\",\n" +
            "mod: \"8cfc37515d0e9b594360fb810ec8f8e32f7f932a5cc79429b3861b1df4d0b3d68eee1ce03123e1e40a70b72b00b866dabbd720ae0beb4bf8e7dc1233d1e1b72b8c3a7e683f882c692c3df1c53fe1f05769216637ef6b23f6ca654fe0f704cbd22496619e66558a5c1f9ea048b0e35a4ed6a62a4e8b7ea5ad67e5f0fb272a133e4cc8be924ea33761060282276caa992392646bdf29a90307a72d0ac8338accca0cfb43d63f604c7767b7fda3692a4493\",\n" +
            "exponent: \"03\",\n" +
            "issuer: \"TPN\",\n" +
            "key_length: 1984,\n" +
            "sha1: \"b402cf25762276cbb9dc20e40b9a7dee2cb48f51\",\n" +
            "exp: \"20300101\"" +
            "}," +
            "{" +
            "rid: \"A000000677\",\n" +
            "capk_index: \"04\",\n" +
            "mod: \"88bc4cae126fab559b8022cadae9b6d258a348116317584dbce8fd1b6e2fc1d8d8a3aef8d09bb5608c959410811f6cdd571989c0e2c85f91441d633690d92cf6f981208884362d5c554d671b00723994bd4c145c1648fb872f407f5f14e91b22be54aa7a2d6bb7900cabac008abe55a52a71c600a666b203ee51003967bb696ae1310e78a73bb89bbda224918f3305a10cec165f345de418a1a0a9772b9fa367c3ddd5af6b9bb769addaed6ee6a57f9caf7a3f99617b4b32daed5b0582d61fc27a34e7b1af683c51f44d7fece66181e673f6badd4ae70074fca65599335fa581fd9726739218d8d079786390a547b2ad4bf697b9c11081e5\",\n" +
            "exponent: \"03\",\n" +
            "issuer: \"TPN\",\n" +
            "key_length: 1984,\n" +
            "sha1: \"35fa471dd4058cf4319b59032d58cfbce4eed62a\",\n" +
            "exp: \"20300101\"" +
            "}," +
            "{" +
            "rid: \"A000000677\",\n" +
            "capk_index: \"05\",\n" +
            "mod: \"869D8F57D908ECFC726CCB4BFDCB4831C7E0166281BC482C552D19687CCA62D31AB51CB48D8FA9A1767EE9DEF4D3FD207A17D1FFFA594797A7A291AF1420887133DADBFC171DC57E163BDA40114C02E98D83CA4CC992D7619B684367C2163C55BD09091A777E1FBFFEEB47194A50AA76307D6C77E52339277B29CD41D100001D8BB0AF6D13F171EB83874E3005A313C91CED6A9C08D4E11DBE94324519284EF02758C536BE05980134D885C10CD3485C590B496E58FDC29F0C120F73167C3E39AD0C5ED3C0F7302B5DB831251C8A7B3F3CD07D5CB9D337B254A2F5DBCC92CCAC5B8AE825FFA47392A0BD42795C579C0BDD43B6FAFC0F1271\",\n" +
            "exponent: \"03\",\n" +
            "issuer: \"TPN\",\n" +
            "key_length: 1984,\n" +
            "sha1: \"EBEE8CC68E6CB50CB1E309864500849065C29C96\",\n" +
            "exp: \"20351231\"" +
            "}," +
            "{" +
            "rid: \"A000000677\",\n" +
            "capk_index: \"09\",\n" +
            "mod: \"B9FBF017A08F6F3AA6F43DD4AB1D45E1CFAF8A3507D99BC4965675BC962CBC99484DE2C40DFD4AEA34825C4094C744162DCC1F26C584BED09E3720608D4693C18ACEEEF3433413AC72D392C6A7FD6F3AFA724171C316390C91DEEC27C203BF1AD6162F1086C67668E72CAEE448273F9AB7621265D3341BD42927541FF8862D5F629E8421B8416BFC07CC6F710F2CFFDBF93DA29A086B443CBB4427D5EC4D619976A11FE99ED56467A3F0FC5192EB907F\",\n" +
            "exponent: \"03\",\n" +
            "issuer: \"TPN\",\n" +
            "key_length: 1984,\n" +
            "sha1: \"EBEE8CC68E6CB50CB1E309864500849065C29C96\",\n" +
            "exp: \"20351231\"" +
            "}" +
            "]" +
            "}"
}

fun UpdateDatabaseConfigCapk(realm: Realm) {
    realm.beginTransaction()
    realm.delete(ConfigCapkRO::class.java)
    val config_capks = JSONObject(GetConfigCapkStringJson())
    Log.v("TEST", "json: ${config_capks.getJSONArray("config_capk")}")
    val list_config_capk_json = config_capks.getJSONArray("config_capk")
    for (i in 0 until list_config_capk_json.length()) {
        val ccapk = list_config_capk_json.getJSONObject(i)
        Log.v("TEST", "ccapk: $ccapk")
        val config_capk = ConfigCapkRO(
            rid = ccapk.getString("rid"),
            capk_index = ccapk.getString("capk_index"),
            mod = ccapk.getString("mod"),
            exponent = ccapk.getString("exponent"),
            issuer = ccapk.getString("issuer"),
            key_length = ccapk.getInt("key_length"),
            sha1 = ccapk.getString("sha1"),
            exp = ccapk.getString("exp"),
        )
        realm.copyToRealm(config_capk)
    }
    realm.commitTransaction()
}

fun UpdateAID(realm: Realm) {
    realm.beginTransaction()
    realm.delete(ConfigAIDRO::class.java)
    val config_aid = JSONObject(getAIDStrinJson())
    Log.v("TEST", "json: ${config_aid.getJSONArray("config_aid")}")
    val list_config_aid_json = config_aid.getJSONArray("config_aid")
    for (i in 0 until list_config_aid_json.length()) {
        val aid = list_config_aid_json.getJSONObject(i)
        Log.v("TEST", "aid: $aid")

        val config_AID = ConfigAIDRO(
            aid_index = aid.getString("aid_index"),
            card_brand = aid.getString("card_brand"),
            aid = aid.getString("aid"),
            aid_version = aid.getString("aid_version"),
            app_select = aid.getString("app_select"),
            app_priority = aid.getString("app_priority"),
            threshold = aid.getString("threshold"),
            target_percent = aid.getString("target_percent"),
            max_target_percent = aid.getString("max_target_percent"),
            floor_limit = aid.getString("floor_limit"),
            tac_default = aid.getString("tac_default"),
            tac_denial = aid.getString("tac_denial"),
            tac_online = aid.getString("tac_online"),
            terminal_country_code = aid.getString("terminal_country_code"),
            terminal_currency_code = aid.getString("terminal_currency_code"),
            terminal_capability = aid.getString("terminal_capability"),
            add_terminal_capability = aid.getString("add_terminal_capability"),
            terminal_type = aid.getString("terminal_type"),
            contactless_transactionlimit = aid.getString("contactless_transactionlimit"),
            contacless_cvmlimit = aid.getString("contacless_cvmlimit"),
            contactless_floorlimit = aid.getString("contactless_floorlimit"),
            contactless_default = aid.getString("contactless_default"),
            contactless_denail = aid.getString("contactless_denail"),
            contactless_online = aid.getString("contactless_online"),
        )
        realm.copyToRealm(config_AID)
    }
    realm.commitTransaction()
}

fun getAIDStrinJson(): String {
    return "{" +
            "config_aid: [" +
            "{" +
            "aid_index: \"1\"," +
            "card_brand: \"VISA CARD\"," +
            "aid: \"A000000003\"," +
            //"aid: \"A0000000031010\"," +
            "aid_version: \"0096\"," +
            "app_select: \"00\"," +
            "app_priority: \"02\"," +
            "threshold: \"000000000010\"," +
            "target_percent: \"90\"," +
            "max_target_percent: \"01\"," +
            "floor_limit:  \"00000000\"," +
            "tac_default: \"DC4000A800\"," +
            "tac_denial: \"0010000000\"," +
            "tac_online: \"DC4004F800\"," +
            "terminal_country_code: \"0764\"," +
            "terminal_currency_code: \"0764\"," +
            "terminal_capability: \"E0B8C8\"," +
            "add_terminal_capability: \"E080F0A001\"," +
            "terminal_type: \"22\"," +
            "contactless_transactionlimit: \"000000000999\"," +
            "contacless_cvmlimit: \"000099999999\"," +
            "contactless_floorlimit: \"000099999999\"," +
            "contactless_default: \"\"," +
            "contactless_denail: \"\"," +
            "contactless_online: \"\"" +
            "}," +
            "{" +
            "aid_index: \"2\"," +
            "card_brand: \"Master Card\"," +
            "aid: \"A0000000041010\"," +
            "aid_version: \"0096\"," +
            "app_select: \"00\"," +
            "app_priority: \"02\"," +
            "threshold: \"000000000010\"," +
            "target_percent: \"90\"," +
            "max_target_percent: \"01\"," +
            "floor_limit:  \"00000000\"," +
            "tac_default: \"DC4000A800\"," +
            "tac_denial: \"0010000000\"," +
            "tac_online: \"DC4004F800\"," +
            "terminal_country_code: \"0764\"," +
            "terminal_currency_code: \"0764\"," +
            "terminal_capability: \"E0F8C8\"," +
            "add_terminal_capability: \"6000F0A001\"," +
            "terminal_type: \"22\"," +
            "contactless_transactionlimit: \"000000000999\"," +
            "contacless_cvmlimit: \"000099999999\"," +
            "contactless_floorlimit: \"000099999999\"," +
            "contactless_default: \"F45084800C\"," +
            "contactless_denail: \"0000000000\"," +
            "contactless_online: \"F45084800C\"" +
            "}," +
            "{" +
            "aid_index: \"3\"," +
            "card_brand: \"AMEX\"," +
            "aid: \"A00000002501\"," +
            "aid_version: \"0096\"," +
            "app_select: \"00\"," +
            "app_priority: \"02\"," +
            "threshold: \"000000000010\"," +
            "target_percent: \"90\"," +
            "max_target_percent: \"01\"," +
            "floor_limit:  \"00000000\"," +
            "tac_default: \"DC4000A800\"," +
            "tac_denial: \"0010000000\"," +
            "tac_online: \"DC4004F800\"," +
            "terminal_country_code: \"0764\"," +
            "terminal_currency_code: \"0764\"," +
            "terminal_capability: \"E0F8C8\"," +
            "add_terminal_capability: \"6000F0A001\"," +
            "terminal_type: \"22\"," +
            "contactless_transactionlimit: \"000000000999\"," +
            "contacless_cvmlimit: \"000099999999\"," +
            "contactless_floorlimit: \"000099999999\"," +
            "contactless_default: \"F45084800C\"," +
            "contactless_denail: \"0000000000\"," +
            "contactless_online: \"F45084800C\"" +
            "}," +
            "{" +
            "aid_index: \"4\"," +
            "card_brand: \"JCB UL tool\"," +
            "aid: \"A0000000651010\"," +
            "aid_version: \"0096\"," +
            "app_select: \"00\"," +
            "app_priority: \"02\"," +
            "threshold: \"100001111010\"," +
            "target_percent: \"90\"," +
            "max_target_percent: \"01\"," +
            "floor_limit:  \"00000000\"," +
            "tac_default: \"DC4000A800\"," +
            "tac_denial: \"0010000000\"," +
            "tac_online: \"DC4004F800\"," +
            "terminal_country_code: \"0764\"," +
            "terminal_currency_code: \"0764\"," +
            "terminal_capability: \"E0F8C8\"," +
            "add_terminal_capability: \"6000F0A001\"," +
            "terminal_type: \"22\"," +
            "contactless_transactionlimit: \"000000000999\"," +
            "contacless_cvmlimit: \"000099999999\"," +
            "contactless_floorlimit: \"000099999999\"," +
            "contactless_default: \"F45084800C\"," +
            "contactless_denail: \"0000000000\"," +
            "contactless_online: \"F45084800C\"" +
            "}," +
            "{" +
            "aid_index: \"5\"," +
            "card_brand: \"TPN\"," +
            "aid: \"A000000677010101\"," +
            "aid_version: \"0020\"," +
            "app_select: \"00\"," +
            "app_priority: \"02\"," +
            "threshold: \"100001111010\"," +
            "target_percent: \"90\"," +
            "max_target_percent: \"01\"," +
            "floor_limit:  \"00000000\"," +
            "tac_default: \"DC4000A800\"," +
            "tac_denial: \"0010000000\"," +
            "tac_online: \"DC4004F800\"," +
            "terminal_country_code: \"0764\"," +
            "terminal_currency_code: \"0764\"," +
            "terminal_capability: \"E0F8C8\"," +
            "add_terminal_capability: \"6000F0A001\"," +
            "terminal_type: \"22\"," +
            "contactless_transactionlimit: \"000000000999\"," +
            "contacless_cvmlimit: \"000099999999\"," +
            "contactless_floorlimit: \"000099999999\"," +
            "contactless_default: \"F45084800C\"," +
            "contactless_denail: \"0000000000\"," +
            "contactless_online: \"F45084800C\"" +
            "}," +
            "{" +
            "aid_index: \"6\"," +
            "card_brand: \"TPN\"," +
            "aid: \"A000000677010102\"," +
            "aid_version: \"0020\"," +
            "app_select: \"00\"," +
            "app_priority: \"02\"," +
            "threshold: \"100001111010\"," +
            "target_percent: \"90\"," +
            "max_target_percent: \"01\"," +
            "floor_limit:  \"00000000\"," +
            "tac_default: \"DC4000A800\"," +
            "tac_denial: \"0010000000\"," +
            "tac_online: \"DC4004F800\"," +
            "terminal_country_code: \"0764\"," +
            "terminal_currency_code: \"0764\"," +
            "terminal_capability: \"E0F8C8\"," +
            "add_terminal_capability: \"6000F0A001\"," +
            "terminal_type: \"22\"," +
            "contactless_transactionlimit: \"000000000999\"," +
            "contacless_cvmlimit: \"000099999999\"," +
            "contactless_floorlimit: \"000099999999\"," +
            "contactless_default: \"F45084800C\"," +
            "contactless_denail: \"0000000000\"," +
            "contactless_online: \"F45084800C\"" +
            "}," +
            "{" +
            "aid_index: \"7\"," +
            "card_brand: \"TPN\"," +
            "aid: \"A000000677010103\"," +
            "aid_version: \"0020\"," +
            "app_select: \"00\"," +
            "app_priority: \"02\"," +
            "threshold: \"100001111010\"," +
            "target_percent: \"90\"," +
            "max_target_percent: \"01\"," +
            "floor_limit:  \"00000000\"," +
            "tac_default: \"DC4000A800\"," +
            "tac_denial: \"0010000000\"," +
            "tac_online: \"DC4004F800\"," +
            "terminal_country_code: \"0764\"," +
            "terminal_currency_code: \"0764\"," +
            "terminal_capability: \"E0F8C8\"," +
            "add_terminal_capability: \"6000F0A001\"," +
            "terminal_type: \"22\"," +
            "contactless_transactionlimit: \"000000000999\"," +
            "contacless_cvmlimit: \"000099999999\"," +
            "contactless_floorlimit: \"000099999999\"," +
            "contactless_default: \"F45084800C\"," +
            "contactless_denail: \"0000000000\"," +
            "contactless_online: \"F45084800C\"" +
            "}" +
            "]" +
            "}"
}

fun UpdateTransactionCounter(realm: Realm) {
//    realm.beginTransaction()
//    realm.delete(TransactionSequentCounter::class.java)
    val counter = JSONObject(getTransactionCounterStrinJson())
    realm.executeTransaction {
        if (counter != null) {

            val config_counter = TransactionSequentCounter(
                counter = counter.getString("counter").toInt()
            )
            realm.copyToRealm(config_counter)
        }
    }

//    }
    // realm.commitTransaction()
}

fun getTransactionCounterStrinJson(): String {
    return "{" + "counter: 0" + "}"
}