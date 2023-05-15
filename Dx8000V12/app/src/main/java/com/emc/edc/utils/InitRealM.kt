package com.emc.edc.utils

import android.content.Context
import android.util.Log
import com.emc.edc.*
import com.emc.edc.database.DataSettingRO
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where

fun InitRealM(context: Context) {
    Realm.init(context)
    val mustUpdateConfigTerminalRO = true
    val mustUpdateProcessingCodeRO = true
    val mustUpdateTransactionRO = true
    val mustUpdateConfigCardRO = true
    val mustUpdateConfigCardControlRO = true
    val mustUpdateConfigHostRO = true
    val mustUpdateConfigCapkRO = true
    val mustUpdateconfigAID = true
    val mustUpdateTransactionSequentCounter = true
    try {
        val dataSettingApp = Realm.getInstance(dataSettingConfiguration)
        val dataSetting = dataSettingApp.where<DataSettingRO>().findFirst()
        Log.v("TEST", "Get setting value: $dataSetting")
        if (dataSetting == null) {
            dataSettingApp.beginTransaction()
            val data_setting = DataSettingRO()
            dataSettingApp.copyToRealm(data_setting)
            dataSettingApp.commitTransaction()
        }
        dataSettingApp.close()
    } catch (e: Exception) {
        Log.v("TEST", e.toString())
    }

    var dataRealmRealmConfiguration: RealmConfiguration

    try {
        dataRealmRealmConfiguration = updateConfigDataRealmConfiguration
        Realm.getInstance(updateConfigDataRealmConfiguration)
    } catch (e: Exception) {
        Log.v("TEST", "error: $e")
        val EvtsUpdateRO: ArrayList<EvtUpdateRo> = GetListUpdateRO(e.toString())
        Log.v("TEST", EvtsUpdateRO.toString())

        Realm.getInstance(dataSettingConfiguration).use { dataSettingRealm ->
            dataSettingRealm.executeTransaction {
                val data = dataSettingRealm.where<DataSettingRO>().findFirst()
                Log.v("TEST", "data setting: $data")
                data!!.ver_schema_config_data = getSchemaConfigData() + 1
            }
        }

        dataRealmRealmConfiguration = RealmConfiguration.Builder()
            .name("config.realm")
            .schemaVersion(getSchemaConfigData())
            .migration(RealmMigrations(EvtsUpdateRO))
            .modules(DataConfigModule())
            .allowWritesOnUiThread(true)
            .build()!!
        Log.v(
            "TEST",
            "realm version: ${Realm.getInstance(dataRealmRealmConfiguration).version}"
        )
    }
    if (mustUpdateConfigTerminalRO) {
        val realm = Realm.getInstance(dataRealmRealmConfiguration)
        UpdateConfigTerminal(realm)
        realm.close()
    }
    if (mustUpdateProcessingCodeRO) {
        val realm = Realm.getInstance(dataRealmRealmConfiguration)
        UpdateDatabaseProcessingCode(realm)
        realm.close()
    }
    if (mustUpdateTransactionRO) {
        val realm = Realm.getInstance(dataRealmRealmConfiguration)
        UpdateDatabaseTransaction(realm)
        realm.close()
    }
    if (mustUpdateConfigCardRO) {
        val realm = Realm.getInstance(dataRealmRealmConfiguration)
        UpdateDatabaseConfigCard(realm)
        realm.close()
    }
    if (mustUpdateConfigCardControlRO) {
        val realm = Realm.getInstance(dataRealmRealmConfiguration)
        UpdateDatabaseConfigCardControl(realm)
        realm.close()
    }
    if (mustUpdateConfigHostRO) {
        val realm = Realm.getInstance(dataRealmRealmConfiguration)
        UpdateDatabaseConfigHost(realm)
        realm.close()
    }
    if (mustUpdateConfigCapkRO) {
        val realm = Realm.getInstance(dataRealmRealmConfiguration)
        UpdateDatabaseConfigCapk(realm)
        realm.close()
    }
    if (mustUpdateconfigAID) {
        val realm = Realm.getInstance(dataRealmRealmConfiguration)
        UpdateAID(realm)
        realm.close()
    }
    if (mustUpdateTransactionSequentCounter) {
        val realm = Realm.getInstance(dataRealmRealmConfiguration)
        UpdateTransactionCounter(realm)
        realm.close()
    }


    try {
        Realm.getInstance(transactionDataConfiguration)
    } catch (e: Exception) {
        Log.v("TEST", "error: $e")
        val EvtsUpdateRO: ArrayList<EvtUpdateRo> = GetListUpdateRO(e.toString())
        Log.v("TEST", EvtsUpdateRO.toString())

        Realm.getInstance(dataSettingConfiguration).use { dataSettingRealm ->
            dataSettingRealm.executeTransaction {
                val data = dataSettingRealm.where<DataSettingRO>().findFirst()
                Log.v("TEST", "data setting: $data")
                data!!.ver_schema_transaction_data = getSchemaTransactionData() + 1
            }
        }
        val transactionDataConfigurationUpdated = RealmConfiguration.Builder()
            .name("transaction.realm")
            .schemaVersion(getSchemaTransactionData())
            .migration(RealmMigrations(EvtsUpdateRO))
            .modules(DataTransactionModule())
            .allowWritesOnUiThread(true)
            .build()!!

        val transactionRealm = Realm.getInstance(transactionDataConfigurationUpdated)

        Log.v("TEST", "realm version: ${transactionRealm.version}")
    }
}