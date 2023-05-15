package com.emc.edc.database

import io.realm.RealmObject

open class TransactionSequentCounter(var counter: Int = 1):RealmObject() {

    fun isList(type: String): Boolean = when (type) {
        "" -> true
        else -> false
    }

    fun getType(type: String): Class<*> = when (type) {
        "transaction_Sequence_Counter"
        -> Int::class.java
        else -> String::class.java
    }

}



