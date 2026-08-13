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

            LaunchedEffect(viewModel.preamp) {
                audioService?.audioEngine?.setPreGain(viewModel.preamp)
            }

            LaunchedEffect(
                viewModel.limiterEnabled,
                viewModel.limiterThreshold,
                viewModel.limiterAttack,
                viewModel.limiterRelease,
                viewModel.limiterRatio,
                viewModel.limiterPostGain
            ) {
                audioService?.audioEngine?.setLimiter(
                    enabled = viewModel.limiterEnabled,
                    threshold = viewModel.limiterThreshold,
                    attack = viewModel.limiterAttack,
                    release = viewModel.limiterRelease,
                    ratio = viewModel.limiterRatio,
                    postGain = viewModel.limiterPostGain
                )
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0D0D12)
            ) {
                MainScreen(
                    viewModel = viewModel,
                    onToggleEngine = { toggleEngine(viewModel) }
                )
            }
        }
    }

    private fun toggleEngine(viewModel: EqViewModel) {
        if (viewModel.isEngineRunning) {
            audioService?.audioEngine?.setEnabled(false)
            stopService(Intent(this, AudioEngineService::class.java))
            if (bound) {
                unbindService(connection)
                bound = false
            }
            viewModel.isEngineRunning = false
        } else {
            val intent = Intent(this, AudioEngineService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            viewModel.isEngineRunning = true
            Toast.makeText(this, "Zeus EQ Pro18 activado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, AudioEngineService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
}
