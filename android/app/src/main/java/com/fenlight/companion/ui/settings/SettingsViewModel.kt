package com.fenlight.companion.ui.settings

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.BuildConfig
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.update.UpdateChecker
import com.fenlight.companion.data.update.UpdateInfo
import com.fenlight.companion.data.update.UpdateResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class UpdateUiState(
    val checking: Boolean = false,
    val available: Boolean = false,
    val upToDate: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val error: String? = null,
    val downloading: Boolean = false,
)

data class SettingsUiState(
    val checkUpdateOnStartup: Boolean = true,
    val update: UpdateUiState = UpdateUiState(),
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val prefs = app.prefs
    private val checker = UpdateChecker()

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.checkUpdateOnStartup.collect { enabled ->
                _state.update { it.copy(checkUpdateOnStartup = enabled) }
            }
        }
    }

    fun toggleCheckUpdateOnStartup(enabled: Boolean) {
        viewModelScope.launch { prefs.setCheckUpdateOnStartup(enabled) }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _state.update { it.copy(update = UpdateUiState(checking = true)) }
            _state.update {
                it.copy(
                    update = when (val result = checker.check(BuildConfig.VERSION_CODE)) {
                        is UpdateResult.Available -> UpdateUiState(available = true, updateInfo = result.info)
                        UpdateResult.UpToDate -> UpdateUiState(upToDate = true)
                        is UpdateResult.Error -> UpdateUiState(error = result.message)
                    }
                )
            }
        }
    }

    fun downloadUpdate(apkUrl: String) {
        val context = getApplication<Application>()
        val destFile = File(context.getExternalFilesDir(null), "FenLightCompanion-update.apk")
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("FenLight Companion update")
            .setDescription("Downloading…")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destFile))
        val downloadId = dm.enqueue(request)
        _state.update { it.copy(update = it.update.copy(downloading = true)) }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != downloadId) return
                context.unregisterReceiver(this)
                _state.update { it.copy(update = it.update.copy(downloading = false)) }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    destFile,
                )
                context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
        @Suppress("UnspecifiedRegisterReceiverFlag")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }
}
