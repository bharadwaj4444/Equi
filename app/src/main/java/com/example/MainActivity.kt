package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.EqualizerViewModel
import com.example.ui.screens.EqualizerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        // Request RECORD_AUDIO and POST_NOTIFICATIONS permissions gracefully
        val permissionsLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { /* Granted or denied gracefully */ }

        LaunchedEffect(Unit) {
          val neededPermissions = mutableListOf<String>()
          if (ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
          ) {
            neededPermissions.add(Manifest.permission.RECORD_AUDIO)
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.POST_NOTIFICATIONS
              ) != PackageManager.PERMISSION_GRANTED
            ) {
              neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
          }
          if (neededPermissions.isNotEmpty()) {
            permissionsLauncher.launch(neededPermissions.toTypedArray())
          }
        }

        val viewModel: EqualizerViewModel = viewModel()
        EqualizerScreen(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

