package com.emc.edc

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.NavHostController
import com.emc.edc.screen.amount.AmountScreen
import com.emc.edc.screen.card_entry.CardEntryProcess
import com.emc.edc.screen.enter_password_pin.EnterPasswordPinScreen
import com.emc.edc.screen.sale_selection.SelectOperation
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.coroutines.DelicateCoroutinesApi
import org.json.JSONObject
import com.emc.edc.screen.search_transaction.SearchTransactionScreen
import com.emc.edc.screen.transaction_display.TransactionDisplay

@ExperimentalMaterialApi
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalComposeUiApi
@DelicateCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun Navigation(context: Context, navController: NavHostController) {
    AnimatedNavHost(
        navController, startDestination = Route.Home.route,
    ) {

        composable(
            Route.Home.route,
            exitTransition = { ->
                slideOutHorizontally(
                    targetOffsetX = { -1000 },
                )
            },
            popEnterTransition = { ->
                slideInHorizontally(
                    initialOffsetX = { -1000 },
                )
            },

            ) {
            MainScreen(navController, context)
        }
        composable(
            Route.Amount.route + "/{data}",
            enterTransition = { ->
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                )
            },
            exitTransition = { ->
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                )
            },
            popEnterTransition = { ->
                slideInHorizontally(
                    initialOffsetX = { -1000 },
                )
            },
        ) { navBackStack ->
            val data = JSONObject(navBackStack.arguments?.getString("data"))

            AmountScreen(
                context,
                navController,
                data.toString()
//                data.getString("transaction_type"),
//                data.getString("title")
            )
        }

        composable(
            Route.Select.route + "/{data}",
            enterTransition = { ->
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                )
            },
            exitTransition = { ->
                slideOutHorizontally(
                    targetOffsetX = { -1000 },
                )
            },
        ) { navBackStack ->
            val data = navBackStack.arguments?.getString("data")

            SelectOperation(navController, data!!)
        }
        composable(
            Route.CardEntry.route,
            exitTransition = { ->
                slideOutHorizontally(
                    targetOffsetX = { -1000 },
                )
            },
            popEnterTransition = { ->
                slideInHorizontally(
                    initialOffsetX = { -1000 },
                )
            },

            ) {
            MainScreen(navController, context)
        }
        composable(
            Route.CardEntryProcess.route + "/{data}",
            enterTransition = { ->
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                )
            },
            exitTransition = { ->
                slideOutHorizontally(
                    targetOffsetX = { -1000 },
                )
            },
        ) { navBackStack ->
            val data = navBackStack.arguments?.getString("data")

            CardEntryProcess(
                context, navController, data!!
            )
        }
        composable(
            Route.CardDisplay.route + "/{data}",
            enterTransition = { ->
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                )
            },
            exitTransition = { ->
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                )
            },
        ) { navBackStack ->
            val data = navBackStack.arguments?.getString("data")

            //CardDisplay(navController, data!!, context)
        }
        composable(
            Route.EnterPasswordPin.route + "/{data}",
            enterTransition = { ->
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                )
            },
            exitTransition = { ->
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                )
            },
            popEnterTransition = { ->
                slideInHorizontally(
                    initialOffsetX = { -1000 },
                )
            }
        ) { navBackStack ->
            val data = JSONObject(navBackStack.arguments?.getString("data"))
            val route = if (data.has("route")) data.getString("route") else null

            EnterPasswordPinScreen(context, navController, data, route)
        }
        composable(
            Route.SearchTransaction.route + "/{data}",
            enterTransition = { ->
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                )
            },
            exitTransition = { ->
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                )
            },
            popEnterTransition = { ->
                slideInHorizontally(
                    initialOffsetX = { -1000 },
                )
            },
        ) { navBackStack ->
            val data = JSONObject(navBackStack.arguments?.getString("data"))

            SearchTransactionScreen(
                context, navController,
                data.getString("transaction_type"), data.getString("transaction_title")
            )
        }
        composable(
            Route.TransactionDisplay.route + "/{data}",
//            Route.TransactionDisplay.route,
            enterTransition = { ->
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                )
            },
            exitTransition = { ->
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                )
            },
        ) { navBackStack ->
            val data = navBackStack.arguments?.getString("data")
             TransactionDisplay(navController, data!!, context)
        }

    }
}