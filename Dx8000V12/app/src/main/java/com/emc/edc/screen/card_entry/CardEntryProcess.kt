package com.emc.edc.screen.card_entry

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.*
import com.emc.edc.*
import com.emc.edc.R
import com.emc.edc.emv.DeviceHelper
import com.emc.edc.emv.Emv
import com.emc.edc.online_transaction.prepareReversal
import com.emc.edc.online_transaction.sendOnlineTransaction
import com.emc.edc.online_transaction.sendReversal
import com.emc.edc.screen.card_display.cardDisplayConfirmAmount
import com.emc.edc.screen.theme.*
import com.emc.edc.screen.utils.getHostInformationFromCard
import com.emc.edc.utils.buildISO8583
import com.usdk.apiservice.aidl.beeper.UBeeper
import com.usdk.apiservice.aidl.led.ULed
import org.json.JSONObject

import com.usdk.apiservice.aidl.constants.RFDeviceName;
import com.usdk.apiservice.aidl.emv.CandidateAID
import com.usdk.apiservice.aidl.emv.KernelID
import com.usdk.apiservice.aidl.led.Light
import kotlinx.coroutines.*
import java.util.*


@SuppressLint("CoroutineCreationDuringComposition")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@RequiresApi(Build.VERSION_CODES.O)
@DelicateCoroutinesApi

@Composable
fun CardEntryProcess(
    context: Context, navController: NavHostController, data: String
) {
    val jsonData = remember { mutableStateOf(JSONObject(data)) }
    val amount = jsonData.value.optString("amount")
    val transactionType = jsonData.value.optString("transaction_type")
    val title = jsonData.value.optString("title")
    val hasError = remember { mutableStateOf(false) }
    val hasEMVStartAgain = remember { mutableStateOf(false) }
    val hasEMVStartAgainConfirm = remember { mutableStateOf(false) }
    val beeper: UBeeper = DeviceHelper.me().beeper
    val led: ULed = DeviceHelper.me().getLed(RFDeviceName.INNER)
    val requestOnlineStatus = remember { mutableStateOf(false) }
    val responseOnlineStatus = remember { mutableStateOf(false) }
    val endProcessStatus = remember { mutableStateOf(false) }
    val buttonConfirmAmount = remember { mutableStateOf(false) }
    val loading = remember { mutableStateOf(false) }
    val textLoading = remember { mutableStateOf("") }
    val dialogStatus = remember { mutableStateOf(false) }
    val arksToPrintStatus = remember { mutableStateOf(false) }
    val errMessage = remember { mutableStateOf("") }
    val seletAIDPopup = remember { mutableStateOf(false) }
    val aidList: MutableList<String> = remember { mutableStateListOf() }
    val aidOriginalList: MutableList<List<CandidateAID>> = remember { mutableStateListOf() }
    val isConnectionTimeOut = remember { mutableStateOf(false) }
    val countDownTimer = remember { mutableStateOf("60") }
    val scope = rememberCoroutineScope()
    val confirmAmountEnableStatus = remember { mutableStateOf(true) }
    val isSendReversal = remember { mutableStateOf(false) }
    val isReversalResponse = remember { mutableStateOf(false) }
    val hasErrorReversal = remember { mutableStateOf(false) }
    val errMessageReversal = remember { mutableStateOf("") }
    val countTryAgain = remember { mutableStateOf(1) }
    val popupPrintSlipCustomer = remember { mutableStateOf(false) }
    val popUpContinue = remember { mutableStateOf(false) }
    val checkEMVProcess = remember { mutableStateOf(false) }

    val timer = object : CountDownTimer(60000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            if (endProcessStatus.value || hasError.value) {
                cancel()
            }
            countDownTimer.value = ((millisUntilFinished / 1000)).toString()
        }

        override fun onFinish() {
            scope.launch {
                countDownTimer.value = "0"
                DeviceHelper.me().emv.stopEMV()
                DeviceHelper.me().emv.stopSearch()
                navController.popBackStack()
            }
        }
    }

    LaunchedEffect(Unit) {
        timer.start()
        scope.launch {
            startTrade(
                jsonData,
                hasError,
                errMessage,
                navController,
                seletAIDPopup,
                aidList,
                aidOriginalList,
                buttonConfirmAmount,
                checkEMVProcess,
                confirmAmountEnableStatus,
                endProcessStatus,
                hasEMVStartAgain,
                countTryAgain,
                hasEMVStartAgainConfirm
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            timer.cancel()
            scope.launch {
                DeviceHelper.me().emv.stopEMV();
                DeviceHelper.me().emv.stopSearch()
            }
        }
    }

    if (hasEMVStartAgainConfirm.value) {
        countTryAgain.value += 1
        LaunchedEffect(Unit) {
            timer.start()
            scope.launch {
                startTrade(
                    jsonData,
                    hasError,
                    errMessage,
                    navController,
                    seletAIDPopup,
                    aidList,
                    aidOriginalList,
                    buttonConfirmAmount,
                    checkEMVProcess,
                    confirmAmountEnableStatus,
                    endProcessStatus,
                    hasEMVStartAgain,
                    countTryAgain,
                    hasEMVStartAgainConfirm
                )
                hasEMVStartAgainConfirm.value = false
            }
        }
    }

    if (confirmAmountEnableStatus.value) {
        if (buttonConfirmAmount.value) {
            cardDisplayConfirmAmount(
                confirmAmountEnableStatus,
                navController,
                jsonData,
                requestOnlineStatus,
                endProcessStatus,
                hasError,
                errMessage
            )
        }
    }


    if (checkEMVProcess.value) {
        Utils().cardEntryHandle(
            jsonData,
            requestOnlineStatus,
            endProcessStatus,
            errMessage,
            responseOnlineStatus,
            hasError,
            isSendReversal,
            context,
            dialogStatus,
            textLoading,
            popupPrintSlipCustomer,
            popUpContinue,
            confirmAmountEnableStatus,
            buttonConfirmAmount
        )
        checkEMVProcess.value = false
    }


    if (requestOnlineStatus.value) {
        LaunchedEffect(Unit) {
            scope.launch {
                sendOnlineTransaction(
                    context,
                    hasError,
                    errMessage,
                    jsonData,
                    responseOnlineStatus,
                    dialogStatus,
                    isConnectionTimeOut,
                    textLoading,
                    popupPrintSlipCustomer
                )
            }
        }
    }

    /**
     * the process of 2 gen AC for contact on the operation = contact
     * else the operation is magnetic and contactless
     **/
    if (responseOnlineStatus.value && !endProcessStatus.value) {
        if (jsonData.value.optString("process") == "fullEMV") {
            Emv().secondGenAC(jsonData.value) // 2 gen AC for contact
        } else {
            endProcessStatus.value = true // contactless and magnetic
        }
    }

    /**
     * End process of the transaction
     * Out put from gen AC both 1 and 2 gen AC
     * TC is approve
     * ACC is decline
     * */

    if (endProcessStatus.value) {
        LaunchedEffect(Unit) {
            dialogStatus.value = false
            responseOnlineStatus.value = false
            timer.cancel()
            DeviceHelper.me().emv.stopEMV();
            DeviceHelper.me().emv.stopSearch()
        }
    }

    if (isSendReversal.value || isConnectionTimeOut.value) {
        LaunchedEffect(Unit) {

            scope.launch {
                sendReversal(
                    hasErrorReversal,
                    errMessageReversal,
                    jsonData,
                    isReversalResponse,
                    dialogStatus,
                    isConnectionTimeOut,
                    textLoading
                )

                dialogStatus.value = false
                isSendReversal.value = false
                isConnectionTimeOut.value = false

            }
        }
    }

    if (hasError.value && (!isSendReversal.value && !isConnectionTimeOut.value)) {
        if ((isConnectionTimeOut.value) &&
            jsonData.value.optString("process") == "fullEMV"
            && !endProcessStatus.value
        ) {

            Emv().secondGenAC(jsonData.value)

        } else {
            dialogStatus.value = false

            if (hasEMVStartAgain.value) {
                DeviceHelper.me().emv.stopProcess()
                DeviceHelper.me().emv.stopSearch()

                ShowAlertConfirmMessage(
                    description = errMessage,
                    openDialog = hasError,
                    hasEMVStartAgainConfirm = hasEMVStartAgainConfirm,
                    numToTryAgain = countTryAgain
                )

            } else {
                DeviceHelper.me().emv.stopEMV();
                DeviceHelper.me().emv.stopSearch()
                led.turnOn(Light.RED);

                ShowAlertMessage(
                    description = errMessage,
                    openDialog = hasError,
                    navController = navController
                )
            }
        }
    }

    if (popupPrintSlipCustomer.value) {
        AskPrintCustomerSlip(
            openDialog = popupPrintSlipCustomer,
            navController = navController,
            jsonData = jsonData.value,
            context = context,
        )
    }

    if (dialogStatus.value) {
        loading.value = true
        ShowLoading(openDialog = loading, text = textLoading)
    }

    if (!hasError.value) {
        led.turnOff(Light.RED)
    }

    if (seletAIDPopup.value) {
        ShowSelectAIDList("AID Select", seletAIDPopup, aidList, navController, aidOriginalList)
    }

    if (confirmAmountEnableStatus.value &&
        !buttonConfirmAmount.value ||
        !confirmAmountEnableStatus.value
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            ButtonTitleMenuOperationWait(
                jsonData,
                navController,
                countDownTimer,
                timer,
                title,
                scope
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .wrapContentSize(Alignment.Center)
                    .padding(10.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    text = "Please Choose One",
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                )
            }
            "TODO()"

            GetAmountDetail(amount)
            ButtonKeyIn(amount, transactionType, navController, title)
            Spacer(modifier = Modifier.height(20.dp))
            WaitOperationInfo()
        }
    }
}

@Composable
private fun ButtonTitleMenuOperationWait(
    jsonData: MutableState<JSONObject>,
    navController: NavController,
    countDownTimer: MutableState<String>,
    timer: CountDownTimer,
    title: String,
    scope: CoroutineScope,
) {
    Row(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
    ) {
        IconButton(onClick = {

            scope.launch {
                jsonData.value.put("cancel_event", "button")
                DeviceHelper.me().emv.stopProcess()
                DeviceHelper.me().emv.stopSearch()
                DeviceHelper.me().emv.stopEMV()
                navController.popBackStack()
            }

        }) {
            Icon(Icons.Default.ArrowBack, "Menu", tint = Color.Black)
        }
        Text(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            text = title.uppercase(Locale.getDefault()),
            modifier = Modifier.padding(top = 9.dp),
            color = Color.Black
        )
        Spacer(Modifier.weight(1f))
        Text(
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            text = "${countDownTimer.value} S",
            modifier = Modifier.padding(top = 9.dp, end = 20.dp),
            color = Color.Black
        )
    }
}

@Preview
@Composable
private fun WaitOperationInfo() {
    val compositionCardSwipe by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.swipe))
    val progressCardSwipe by animateLottieCompositionAsState(
        composition = compositionCardSwipe,
        iterations = LottieConstants.IterateForever
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = green130
            ),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.End

    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
//            Column(
//                modifier = Modifier.width(150.dp)
//            ) {

            Text(
                text = "INSERT",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,

                )
//            }

//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.End
//            ) {
            Image(painterResource(R.drawable.poc), "coin1", modifier = Modifier.size(100.dp))
//            }

        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

//            Row(
//                modifier = Modifier
//                    .width(300.dp), horizontalArrangement = Arrangement.Center
//            ) {
            Text(
                text = "TAP",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                modifier = Modifier.padding(30.dp)
            )
            Image(painterResource(R.drawable.pod), "coin1", modifier = Modifier.size(90.dp))
//            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly

        ) {
            Text(
                text = "SWIPE",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
            )
//            Row(
//                modifier = Modifier
//                    .width(400.dp),
//                horizontalArrangement = Arrangement.End
//            ) {

            Image(painterResource(R.drawable.swipe), "coin1", modifier = Modifier.size(130.dp))
        }
//        }
    }

}


@Composable
private fun GetAmountDetail(amount: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            amount.length < 9 -> {
                Text(
                    text = "฿ $amount",
                    color = green100,
                    fontWeight = FontWeight.Bold,
                    fontSize = 45.sp
                )
            }
            else -> {
                Text(
                    text = "฿ $amount",
                    color = green100,
                    fontWeight = FontWeight.Bold,
                    fontSize = 35.sp
                )
            }
        }

    }
}

@Composable
private fun ButtonKeyIn(
    amount: String, transactionType: String,
    navController: NavController,
    title: String
) {
    val jsonData = JSONObject()
    if (amount != "") {
        jsonData.put("amount", amount)
    } else {
        jsonData.put("amount", "")
    }
    jsonData.put("name", " ")
    jsonData.put("transaction_type", transactionType)
    jsonData.put("operation", "key_in")
//    Log.d("TEST",jsonData.toString())

    jsonData.put("title", title)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                DeviceHelper.me().emv.stopSearch()
                navController.navigate("key_in/$jsonData") {
                    popUpTo(Route.Home.route)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = green150),
            shape = RoundedCornerShape(50),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp,
                disabledElevation = 0.dp
            )

        ) {
            Text(
                text = "Manual Input Card Number", fontSize = 10.sp,
                color = Color.White, fontWeight = FontWeight.Normal
            )
        }
    }

}



