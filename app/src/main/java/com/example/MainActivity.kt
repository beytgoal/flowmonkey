package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainAppScreen
import com.example.ui.theme.FlowMonkeyTheme
import com.example.ui.viewmodels.VideoStudioViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      FlowMonkeyTheme {
        val studioViewModel: VideoStudioViewModel = viewModel()
        MainAppScreen(viewModel = studioViewModel)
      }
    }
  }
}
