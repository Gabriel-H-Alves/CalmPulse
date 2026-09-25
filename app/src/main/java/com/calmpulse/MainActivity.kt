package com.calmpulse

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.calmpulse.data.model.UpdateInfo
import com.calmpulse.data.repository.AppUpdateManager
import com.calmpulse.ui.chat.ChatScreen
import com.calmpulse.ui.components.UpdateDialog
import com.calmpulse.ui.components.UpdateDialogState
import com.calmpulse.ui.theme.CalmPulseTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startupPrefs = getSharedPreferences("calmpulse_prefs", Context.MODE_PRIVATE)
        if (startupPrefs.getBoolean("secure_screen_enabled", false)) {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        enableEdgeToEdge()
        setContent {
            val context = this@MainActivity
            val prefs = remember { context.getSharedPreferences("calmpulse_prefs", Context.MODE_PRIVATE) }
            val systemInDark = isSystemInDarkTheme()
            var isDarkTheme by remember {
                mutableStateOf(prefs.getBoolean("dark_mode_enabled", systemInDark))
            }
            var oledDarkMode by remember {
                mutableStateOf(prefs.getBoolean("oled_dark_mode", false))
            }
            var accentColorIndex by remember {
                androidx.compose.runtime.mutableIntStateOf(prefs.getInt("accent_color_index", 0))
            }
            val activeAccent = remember(accentColorIndex) {
                com.calmpulse.ui.theme.CalmPulseAccentPalettes.getOrElse(accentColorIndex) {
                    com.calmpulse.ui.theme.CalmPulseAccentPalettes[0]
                }.second
            }

            val toggleTheme = {
                val newMode = !isDarkTheme
                isDarkTheme = newMode
                prefs.edit().putBoolean("dark_mode_enabled", newMode).apply()
            }

            // ── Auto-Update State ──
            val updateManager = remember { AppUpdateManager(this@MainActivity) }
            val scope = rememberCoroutineScope()
            var showUpdateDialog by remember { mutableStateOf(false) }
            var updateDialogState by remember { mutableStateOf<UpdateDialogState?>(null) }
            var currentUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            var downloadedApk by remember { mutableStateOf<File?>(null) }

            // Verificação silenciosa em segundo plano: não bloqueia nem interrompe o usuário em crise
            LaunchedEffect(Unit) {
                val update = updateManager.checkForUpdate()
                if (update != null) {
                    currentUpdateInfo = update
                    updateDialogState = UpdateDialogState.Available(update)
                    // Não forçamos showUpdateDialog = true no início para preservar a tranquilidade imediata do usuário
                }
            }

            CalmPulseTheme(
                darkTheme = isDarkTheme,
                oledDark = oledDarkMode,
                accentColor = activeAccent
            ) {
                ChatScreen(
                    isDarkTheme = isDarkTheme,
                    oledDarkMode = oledDarkMode,
                    accentColor = activeAccent,
                    onToggleTheme = toggleTheme,
                    onToggleOled = {
                        val newOled = !oledDarkMode
                        oledDarkMode = newOled
                        prefs.edit().putBoolean("oled_dark_mode", newOled).apply()
                    },
                    onSelectAccent = { newIdx ->
                        accentColorIndex = newIdx
                        prefs.edit().putInt("accent_color_index", newIdx).apply()
                    },
                    onCheckUpdate = {
                        scope.launch {
                            Toast.makeText(this@MainActivity, "Buscando atualizações...", Toast.LENGTH_SHORT).show()
                            val update = updateManager.checkForUpdate()
                            if (update != null) {
                                currentUpdateInfo = update
                                updateDialogState = UpdateDialogState.Available(update)
                                showUpdateDialog = true
                            } else {
                                Toast.makeText(this@MainActivity, "Você já está na versão mais recente!", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )

                // Dialog de atualização
                if (showUpdateDialog && updateDialogState != null) {
                    UpdateDialog(
                        state = updateDialogState!!,
                        currentVersionName = BuildConfig.VERSION_NAME,
                        onDismiss = {
                            showUpdateDialog = false
                        },
                        onUpdate = {
                            // Iniciar download
                            currentUpdateInfo?.let { info ->
                                updateDialogState = UpdateDialogState.Downloading(0)
                                scope.launch {
                                    updateManager.downloadApk(info.downloadUrl, info.expectedSha256).collect { state ->
                                        when (state) {
                                            is AppUpdateManager.DownloadState.Downloading -> {
                                                updateDialogState =
                                                    UpdateDialogState.Downloading(state.progress)
                                            }
                                            is AppUpdateManager.DownloadState.Completed -> {
                                                downloadedApk = state.file
                                                updateDialogState =
                                                    UpdateDialogState.ReadyToInstall
                                            }
                                            is AppUpdateManager.DownloadState.Error -> {
                                                updateDialogState =
                                                    UpdateDialogState.Error(state.message)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onInstall = {
                            downloadedApk?.let { apk ->
                                try {
                                    updateManager.installApk(apk)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Erro ao instalar: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
