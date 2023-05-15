package com.emc.edc

import android.annotation.SuppressLint
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.system.Os.close
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.emc.edc.database.DataSettingRO
import com.emc.edc.emv.DeviceHelper
import com.emc.edc.screen.home.MenuView
import com.emc.edc.screen.theme.EmcTheme
import com.emc.edc.utils.InitRealM
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.internal.network.NetworkStateReceiver
import io.realm.kotlin.where
import kotlinx.coroutines.DelicateCoroutinesApi


@ExperimentalFoundationApi
@ExperimentalAnimationApi
@DelicateCoroutinesApi
@ExperimentalComposeUiApi
@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity(), DeviceHelper.ServiceReadyListener {
    @OptIn(ExperimentalMaterialApi::class)
    @SuppressLint("CoroutineCreationDuringComposition")
    @CallSuper
    @RequiresApi(Build.VERSION_CODES.O)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //.................................................................
        //#init realm
        InitRealM(this)
        DeviceHelper.me().init(this)
        DeviceHelper.me().bindService()
        DeviceHelper.me().setServiceListener(this)

        setContent {

            EmcTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background

                ) {
                    val navController = rememberAnimatedNavController()
                    Navigation(this, navController)
                }
            }
        }

    }

    override fun onReady(version: String?) {
        DeviceHelper.me().register(true)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            DeviceHelper.me().unregister()
            DeviceHelper.me().unbindService()
            DeviceHelper.me().setServiceListener(null)
        } catch (e: IllegalStateException) {
            Log.d("test", "unregister fail: " + e.message)
        }
    }
}


@Composable
fun Greeting(name: String) {
    Text("$name")
}


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("CoroutineCreationDuringComposition", "UnusedMaterial3ScaffoldPaddingParameter")
@DelicateCoroutinesApi
//@ExperimentalMaterialApi
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalFoundationApi
@Composable
fun MainScreen(
    navController: NavHostController, context: Context
) {
    //DeviceHelper.me().emv
    val toggleTheme: () -> Unit = {}
    val currentTheme = remember {
        mutableStateOf(false)
    }
    MenuView(navController, context, toggleTheme, currentTheme)
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    EmcTheme {
        Greeting("Android")
    }
}