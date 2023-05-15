package com.emc.edc.database

import io.realm.RealmObject

open class ConfigAIDRO(
    var aid_index: String? = null,
    var card_brand: String? = null,
    var aid: String? = null,
    var aid_version: String? = null,
    var app_select: String? = null,
    var app_priority: String? = null,
    var threshold: String? = null,
    var target_percent: String? = null,
    var max_target_percent: String? = null,
    var floor_limit: String? = null,
    var tac_default: String? = null,
    var tac_denial: String? = null,
    var tac_online: String? = null,
    var terminal_country_code: String? = null,
    var terminal_currency_code : String? = null,
    var terminal_capability: String? = null,
    var add_terminal_capability: String? = null,
    var terminal_type: String? = null,
    var contactless_transactionlimit: String? = null,
    var contacless_cvmlimit: String? = null,
    var contactless_floorlimit: String? = null,
    var contactless_default: String? = null,
    var contactless_denail: String? = null,
    var contactless_online: String? = null,

) : RealmObject() {
    fun isList(type: String): Boolean = when (type) {
        "" -> true
        else -> false
    }

    fun getType(type: String): Class<*> = when (type) {
        "aid_index", "card_brand", "aid", "aid_version", "app_select", "app_priority", "threshold",
        "target_percent", "max_target_percent", "floor_limit", "tac_default", "tac_denial",
        "tac_online", "terminal_country_code","terminal_curency_code ", "terminal_capability",
        "add_terminal_capability", "terminal_type", "contactless_translimit", "contacless_cvmlimit",
        "contactless_floorlmt", "contactless_default", "contactless_denail", "contactless_online"
        -> String::class.java
        else -> String::class.java
    }
}
