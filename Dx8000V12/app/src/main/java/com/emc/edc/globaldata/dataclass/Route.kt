package com.emc.edc.globaldata.dataclass

data class Route(
    val group: String,
    val title: String,
    val path: String,
    val popup: Boolean,
    val txnType: String,
    val txnStatus: String,
    val image: Int,
    val process: String
    )
