package com.calmpulse.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Gerencia verificação, download e validação criptográfica de integridade de atualizações
 * via GitHub Releases conforme OWASP MASVS-CODE-4 e CWE-494.
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
     * Consulta a API do GitHub Releases para checar se há uma versão mais nova que a instalada.
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "$GITHUB_API_BASE/$REPO/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "CalmPulse-Android/${BuildConfig.VERSION_NAME}")
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

            // Extrair hash SHA-256 publicado na release para validação de integridade (SEC-003)
            val sha256Regex = Regex("""(?i)SHA256\s*=\s*([a-f0-9]{64})""")
            val shaMatch = sha256Regex.find(releaseBody)
            val expectedSha256 = shaMatch?.groupValues?.get(1)?.lowercase()

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
                releaseNotes = releaseBody
                    .replace(versionCodeRegex, "")
                    .replace(sha256Regex, "")
                    .trim(),
                expectedSha256 = expectedSha256
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar atualização", e)
            null
        }
    }

    /**
     * Baixa o APK da URL com verificação de progresso e validação de integridade SHA-256.
     */
    fun downloadApk(downloadUrl: String, expectedSha256: String? = null): Flow<DownloadState> = flow {
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

            // Validação de Integridade Criptográfica SHA-256 (SEC-003)
            if (!expectedSha256.isNullOrBlank()) {
                val computedHash = calculateSha256(apkFile)
                if (!computedHash.equals(expectedSha256, ignoreCase = true)) {
                    apkFile.delete()
                    Log.e(TAG, "Hash SHA-256 divergente! Esperado: $expectedSha256, Obtido: $computedHash")
                    emit(DownloadState.Error("Falha de integridade: o arquivo baixado foi corrompido ou adulterado."))
                    return@flow
                }
                Log.i(TAG, "Integridade criptográfica SHA-256 verificada com sucesso!")
            }

            emit(DownloadState.Downloading(100))
            emit(DownloadState.Completed(apkFile))

        } catch (e: Exception) {
            Log.e(TAG, "Erro no download", e)
            emit(DownloadState.Error("Erro no download: ${e.message}"))
        }
    }

    /**
     * Valida os certificados e abre o instalador do Android para o APK baixado.
     */
    fun installApk(apkFile: File) {
        // Validação de assinatura do pacote antes de abrir o instalador
        if (!verifyApkPackage(apkFile)) {
            Log.e(TAG, "Validação do pacote do APK falhou. Abortando instalação.")
            apkFile.delete()
            return
        }

        // Permissão de fontes desconhecidas no Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }
        }

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
     * Calcula o hash SHA-256 do arquivo baixado.
     */
    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifica se o APK baixado pertence ao mesmo pacote e possui a mesma assinatura digital
     * do app atualmente instalado (SEC-003 / OWASP MASVS-CODE-4).
     */
    @Suppress("DEPRECATION")
    private fun verifyApkPackage(apkFile: File): Boolean {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }

            val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
            if (archiveInfo.packageName != context.packageName) {
                Log.e(TAG, "PackageName divergente! Instalado: ${context.packageName}, APK: ${archiveInfo.packageName}")
                return false
            }

            // Obter assinaturas do pacote instalado
            val currentInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                pm.getPackageInfo(context.packageName, flags)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val archiveSigners = archiveInfo.signingInfo?.apkContentsSigners
                val currentSigners = currentInfo.signingInfo?.apkContentsSigners
                if (archiveSigners != null && currentSigners != null) {
                    val archiveSigs = archiveSigners.map { it.toCharsString() }.toSet()
                    val currentSigs = currentSigners.map { it.toCharsString() }.toSet()
                    if (archiveSigs.intersect(currentSigs).isEmpty()) {
                        Log.e(TAG, "Assinatura digital do APK não coincide com a do app instalado!")
                        return false
                    }
                }
            } else {
                val archiveSignatures = archiveInfo.signatures
                val currentSignatures = currentInfo.signatures
                if (archiveSignatures != null && currentSignatures != null) {
                    val archiveSigs = archiveSignatures.map { it.toCharsString() }.toSet()
                    val currentSigs = currentSignatures.map { it.toCharsString() }.toSet()
                    if (archiveSigs.intersect(currentSigs).isEmpty()) {
                        Log.e(TAG, "Assinatura digital do APK não coincide com a do app instalado!")
                        return false
                    }
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inspecionar pacote ou assinatura do APK", e)
            false
        }
    }

    sealed class DownloadState {
        data class Downloading(val progress: Int) : DownloadState()
        data class Completed(val file: File) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
}
