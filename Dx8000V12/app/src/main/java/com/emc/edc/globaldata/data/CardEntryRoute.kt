package com.emc.edc.globaldata.data

import com.emc.edc.R
import com.emc.edc.globaldata.dataclass.Route

object CardEntryRoute {
    val routeList = listOf(
        Route(
            title = "Record Offline Sale",
            txnType = "offline_sale",
            txnStatus = "offline",
            image = R.drawable.ic_offline_sale,
            path = com.emc.edc.Route.Amount.route,
            popup = false,
            group = "card_entry",
            process = "fullEMV"
        ),
        Route(
            title = "Refund",
            txnType = "refund",
            txnStatus = "",
            image = R.drawable.refund,
            path = com.emc.edc.Route.Amount.route,
            popup = false,
            group = "card_entry",
            process = "partialEMV"
        )
//        Route(
//            title = "Pre-Authorize",
//            txnType = "pre_auth",
//            image = R.drawable.ic_pre_auth,
//            path = th.emerchant.terminal.edc_pos.Route.Amount.route,
//            popup = false,
//            group = "card_entry"
//        ),
//        Route(
//            title = "Balance Inquiry",
//            txnType = "balance_inquiry",
//            image = R.drawable.ic_balance_inquiery,
//            path =  th.emerchant.terminal.edc_pos.Route.WaitOperation.route,
//            popup = false,
//            group = "card_entry"
//        )
    )
}