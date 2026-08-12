package com.zeus.eqpro18

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var audioService: AudioEngineService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioEngineService.LocalBinder
            audioService = binder.getService()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            bound = false
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "Se necesitan permisos de audio para el procesamiento real", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNeededPermissions()

        setContent {
            val viewModel: EqViewModel = viewModel()

            LaunchedEffect(Unit) {
                while (true) {
                    audioService?.audioEngine?.let { engine ->
                        viewModel.spectrum = engine.spectrumData.copyOf()
                        viewModel.isEngineRunning = engine.isEnabled()
                    }
                    delay(50)
                }
            }

            LaunchedEffect(viewModel.bands.toList()) {
                audioService?.audioEngine?.setBands(viewModel.bands.toList())
            }

            LaunchedEffect(
                viewModel.limiterEnabled,
                viewModel.limiterThreshold,
                viewModel.limiterAttack,
                viewModel.limiterRelease,
                view
