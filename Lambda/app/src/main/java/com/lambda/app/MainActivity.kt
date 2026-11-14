package com.lambda.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.lambda.app.ui.LambdaApp
import com.lambda.app.ui.LambdaTheme

class MainActivity : ComponentActivity() {
    private val vm: LambdaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            LambdaTheme { LambdaApp(vm) }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.flush() // make sure everything is written to the phone's storage
    }
}
