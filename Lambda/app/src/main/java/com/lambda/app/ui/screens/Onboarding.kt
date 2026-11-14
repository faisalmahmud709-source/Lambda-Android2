package com.lambda.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lambda.app.LambdaViewModel
import com.lambda.app.R
import com.lambda.app.ui.*

@Composable
fun OnboardingScreen(vm: LambdaViewModel) {
    var name by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(R.drawable.lambda_logo), "Lambda",
            Modifier.size(104.dp).clip(RoundedCornerShape(24.dp))
        )
        Spacer(Modifier.height(20.dp))
        Text("Welcome to Lambda", fontFamily = DisplayFont, fontStyle = FontStyle.Italic, fontSize = 32.sp, color = LC.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            "Today's priorities, daily progress and upcoming deadlines at a glance. Everything is stored only on this phone and works fully offline.",
            color = LC.Muted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp
        )
        Spacer(Modifier.height(26.dp))
        LTextField(name, { name = it }, placeholder = "What should we call you?", label = "Your name")
        Spacer(Modifier.height(14.dp))
        PrimaryButton("Get started", { vm.finishOnboarding(name) }, Modifier.fillMaxWidth(), icon = Icons.Filled.ArrowForward)
        Spacer(Modifier.height(6.dp))
        Text("You can change this later in Settings.", color = LC.Muted, fontSize = 11.sp)
    }
}
