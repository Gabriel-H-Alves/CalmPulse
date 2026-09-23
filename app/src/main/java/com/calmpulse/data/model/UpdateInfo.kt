package com.calmpulse.data.model

/**
 * Informações sobre uma atualização disponível do app.
 */
data class UpdateInfo(
    /** Nome da release (ex: "v1.1.0 - Melhorias no Chat") */
    val releaseName: String,
    /** Nome da versão (ex: "1.1.0") */
    val versionName: String,
    /** Código da versão para comparação numérica */
    val versionCode: Int,
    /** URL direta de download do APK */
    val downloadUrl: String,
    /** Notas da release (changelog) */
    val releaseNotes: String
)
