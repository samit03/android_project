package com.emc.edc.screen.card_display

import android.os.Build
import android.os.CountDownTimer
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.emc.edc.R
import com.emc.edc.emv.DeviceHelper
import com.emc.edc.emv.Emv
import com.emc.edc.screen.theme.BgColor
import com.emc.edc.screen.theme.ChipCard
import com.emc.edc.screen.theme.MenuListTextDark
import com.emc.edc.screen.theme.StaticColorText
import com.emc.edc.screen.utils.getCardInformation
import com.emc.edc.screen.utils.getHostInformationFromCard
import com.emc.edc.utils.*
import kotlinx.coroutines.*
import org.json.JSONObject

@RequiresApi(Build.VERSION_CODES.O)
@DelicateCoroutinesApi
@Composable
fun cardDisplayConfirmAmount(
    confirmAmountEnableStatus: MutableState<Boolean>,
    navController: NavHostController,
    jsonData: MutableState<JSONObject>,
    requestOnlineStatus: MutableState<Boolean>,
    endProcessStatus: MutableState<Boolean>,
    hasError: MutableState<Boolean>,
    errorMessage: MutableState<String>
) {

    val countDownTimer = remember { mutableStateOf("60") }
    val checkEnable = remember { mutableStateOf(false) }
    val timer = object : CountDownTimer(60000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            if (hasError.value || endProcessStatus.value) {
                cancel()
            }
            countDownTimer.value = ((millisUntilFinished / 1000)).toString()
        }

        override fun onFinish() {
            countDownTimer.value = "0"
            navController.popBackStack()
        }
    }
    LaunchedEffect(Unit) {
        timer.start()
    }
    DisposableEffect(Unit) {
        onDispose {
            timer.cancel()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {

        val getHostInformationStatus = getCardInformation(
            jsonData,
            hasError,
            errorMessage
        )
        if (hasError.value) {
            //countDownTimer.value = "0"
            timer.cancel()
        }

        if (getHostInformationStatus) {
            ButtonTitleMenuSaleCard(
                jsonData.value.optString("title").uppercase(),
                navController, countDownTimer, timer
            )
            VirtualCard(
                checkEnable.value,
                jsonData.value.optString("card_number"),
                jsonData.value.optString("pan_masking"),
                jsonData.value.optString("name"),
                jsonData.value.optString("amount")
            )
            if (jsonData.value.optString("amount") != "" && !checkEnable.value) {
                ShowAmount(jsonData.value.optString("amount"))
            }

//            if (checkEnable.value) {
//                ShowAmountLittle(amount = jsonData.value.optString("amount"))
//            }
            ButtonSuccessConfirm(
                checkEnable,
                jsonData,
                requestOnlineStatus,
            )
        } else {
            confirmAmountEnableStatus.value = false
        }
    }
}

@Composable
private fun ButtonTitleMenuSaleCard(
    transactionType: String,
    navController: NavController,
    countDownTimer: MutableState<String>,
    timer: CountDownTimer,
) {
    Row(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
    ) {
        IconButton(onClick = {
            timer.cancel()
            DeviceHelper.me().emv.stopEMV()
            DeviceHelper.me().emv.stopSearch()
            navController.popBackStack()
            //responseOnlineStatus.value = true
        }) {
            Icon(Icons.Default.ArrowBack, "Menu", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            text = transactionType,
            modifier = Modifier.padding(top = 9.dp),
            color = MaterialTheme.colorScheme.onSurface
            //color = if (MaterialTheme.colors.isLight) MainText else MainTextDark
        )
        Spacer(Modifier.weight(1f))
        Text(
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            text = "${countDownTimer.value} S",
            modifier = Modifier.padding(top = 9.dp, end = 20.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun VirtualCard(
    check_enable: Boolean, card_number: String, pan_masking: String, name: String, amount: String,
) {
    val cardMasking = remember { mutableStateOf("") }
    val checkEnable = remember { mutableStateOf(false) }

    if (card_number.length != pan_masking.replace(" ", "").length) {
        return
    }

    cardMasking.value = Utils().cardMasking(card_number, pan_masking)
    Column(
        modifier = Modifier
            .offset(x = 135.dp, y = 0.dp)


    ) {
        if (check_enable) {
            Text(
                modifier = Modifier
                    .background(Color.Blue),
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                text = "฿ $amount",
                color = Color.White,
                textAlign = TextAlign.Center,
                //color = if (MaterialTheme.colors.isLight) MainText else MainTextDark
            )
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = StaticColorText),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .height(220.dp)
            .padding(bottom = 10.dp),
        elevation = CardDefaults.cardElevation(8.dp),
    )
    {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                // horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ChipCard),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .width(50.dp)
                ) {

                }
                Column() {
                    when (Utils().identifyCardScheme(card_number)
                    ) {
                        CardScheme.VISA -> Image(
                            painterResource(R.drawable.ic_visa_inc),
                            "coin",
                            modifier = Modifier
                                .offset(x = 230.dp, y = 0.dp)
                                .size(60.dp)
                        )
                        CardScheme.MASTERCARD -> Image(
                            painterResource(R.drawable.ic_mastercard_logo),
                            "coin", modifier = Modifier
                                .offset(x = 230.dp, y = 0.dp)
                                .size(60.dp)
                        )
                        else -> Image(
                            painter = painterResource(R.drawable.coin),
                            "coin",
                            modifier = Modifier
                                .offset(x = 230.dp, y = 0.dp)
                                .size(60.dp)
                        )
                    }
                    Text(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        text = cardMasking.value,
                        modifier = Modifier.offset(x = (-50).dp, y = 0.dp),
                        fontSize = 17.sp,
                        color = MenuListTextDark
                    )
                    Text(
                        fontWeight = FontWeight.Bold,
                        text = "EXPIRES",
                        modifier = Modifier.offset(x = (-50).dp, y = 20.dp),
                        fontSize = 15.sp,
                        color = MenuListTextDark,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        text = "XX/XX",
                        modifier = Modifier.offset(x = (-50).dp, y = 25.dp),
                        fontSize = 15.sp,
                        color = MenuListTextDark
                    )


                    Text(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        text = if (name != "") name else "KEY IN",
                        modifier = Modifier.offset(x = (-50).dp, y = 45.dp),
                        fontSize = 15.sp,
                        color = MenuListTextDark
                    )


                }

            }

        }
    }
}

@Composable
private fun ShowAmount(amount: String) {
    Column(
        modifier = Modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp)
    ) {
        Text(
            fontSize = 15.sp,
            text = "AMOUNT",
            modifier = Modifier.padding(5.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Divider()
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(top = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(end = 5.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    text = "฿",
                    color = MenuListTextDark
                    //color = if (MaterialTheme.colors.isLight) MainText else MainTextDark
                )
                Text(
                    overflow = TextOverflow.Clip,
                    text = amount,
                    color = StaticColorText,
                    //fontFamily = VoiceInteractor.Prompt,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                )
            }
        }
    }
}

@Composable
private fun ShowAmountLittle(amount: String) {
//    Column(
//       modifier = Modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp)
//    ) {
//        Column(
//            horizontalAlignment = Alignment.Start,
//            modifier = Modifier
//                .padding(10.dp)
//                .fillMaxWidth(),
//        ) {
//            Row(
//                modifier = Modifier.padding(top = 9.dp),
//                verticalAlignment = Alignment.Top
//            ) {
    Text(
        overflow = TextOverflow.Clip,
        text = amount,
        color = StaticColorText,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
    )
//            }
//        }
}
//}

@RequiresApi(Build.VERSION_CODES.O)
@DelicateCoroutinesApi
@Composable
private fun ButtonSuccessConfirm(
    checkEnable: MutableState<Boolean>,
    jsonData: MutableState<JSONObject>,
    requestOnlineStatus: MutableState<Boolean>,
) {
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(true) }
    //val errMessage = remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()
    ) {
        Button(
            onClick = {
                if (jsonData.value.optString("process") == "fullEMV") {
                    scope.launch {
                        Emv().readRecord()
                    }
                } else if (jsonData.value.optString("process") == "partialEMV" &&
                    jsonData.value.optString("gen_ac") == "AAC"
                ) {
                    Emv().stopEMV()
                } else {
                    requestOnlineStatus.value = true
                }
                enabled = false
                checkEnable.value = true
            },
            enabled = enabled,
            colors = ButtonDefaults.textButtonColors(
                containerColor = StaticColorText,
                contentColor = BgColor
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp,
                disabledElevation = 0.dp
            ),
        ) {
            if (enabled) {
                Text("Confirm")
            }


        }
    }


}
