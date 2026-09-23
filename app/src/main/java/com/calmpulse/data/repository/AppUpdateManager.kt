package com.calmpulse.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.calmpulse.BuildConfig
import com.calmpulse.data.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Gerencia verificação e download de atualizações via GitHub Releases.
 *
 * Fluxo:
 * 1. checkForUpdate() → consulta GitHub API → retorna UpdateInfo se há versão nova
 * 2. downloadAndInstall() → baixa APK → emite progresso → dispara instalador
 */
class AppUpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdateManager"
        private const val GITHUB_API_BASE = "https://api.github.com/repos"
        private const val REPO = "Gabriel-H-Alves/CalmPulse"
        private const val APK_FILE_NAME = "calmpulse-update.apk"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Verifica se existe uma versão mais recente no GitHub Releases.
     *
     * A descrição (body) da release deve conter "VERSION_CODE=<numero>"
     * para que a comparação funcione corretamente.
     *
     * @return UpdateInfo se existe atualização, null se já está na última versão.
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$GITHUB_API_BASE/$REPO/releases/latest")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w(TAG, "GitHub API retornou ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "")
            val releaseName = json.optString("name", tagName)
            val releaseBody = json.optString("body", "")

            // Extrair VERSION_CODE da descrição da release
            val versionCodeRegex = Regex("""VERSION_CODE\s*=\s*(\d+)""")
            val matchResult = versionCodeRegex.find(releaseBody)
            val remoteVersionCode = matchResult?.groupValues?.get(1)?.toIntOrNull()

            if (remoteVersionCode == null) {
                Log.w(TAG, "VERSION_CODE não encontrado na descrição da release")
                return@withContext null
            }

            val currentVersionCode = BuildConfig.VERSION_CODE
            Log.d(TAG, "Versão atual: $currentVersionCode, Remota: $remoteVersionCode")

            if (remoteVersionCode <= currentVersionCode) {
                Log.d(TAG, "App está atualizado")
                return@withContext null
            }

            // Procurar o asset .apk na release
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            if (apkUrl.isNullOrBlank()) {
                Log.w(TAG, "Nenhum APK encontrado na release")
                return@withContext null
            }

            // Extrair versionName da tag (remove o "v" se existir)
            val versionName = tagName.removePrefix("v")

            UpdateInfo(
                releaseName = releaseName,
                versionName = versionName,
                versionCode = remoteVersionCode,
                downloadUrl = apkUrl,
                releaseNotes = releaseBody.replace(versionCodeRegex, "").trim()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar atualização", e)
            null
        }
    }

    /**
     * Baixa o APK da URL e emite o progresso (0 a 100).
     *
     * Ao completar, retorna o File do APK baixado.
     */
    fun downloadApk(downloadUrl: String): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))

        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                emit(DownloadState.Error("Download falhou: HTTP ${response.code}"))
                return@flow
            }

            val responseBody = response.body ?: run {
                emit(DownloadState.Error("Resposta vazia do servidor"))
                return@flow
            }

            val totalBytes = responseBody.contentLength()
            val updatesDir = File(context.externalCacheDir, "updates")
            if (!updatesDir.exists()) updatesDir.mkdirs()

            val apkFile = File(updatesDir, APK_FILE_NAME)

            withContext(Dispatchers.IO) {
                responseBody.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    }
                }
            }

            emit(DownloadState.Downloading(100))
            emit(DownloadState.Completed(apkFile))

        } catch (e: Exception) {
            Log.e(TAG, "Erro no download", e)
            emit(DownloadState.Error("Erro no download: ${e.message}"))
        }
    }

    /**
     * Abre o instalador do Android para o APK baixado.
     */
    fun installApk(apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
    }

    /**
     * Estados possíveis durante o download do APK.
     */
    sealed class DownloadState {
        data class Downloading(val progress: Int) : DownloadState()
        data class Completed(val file: File) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
}
