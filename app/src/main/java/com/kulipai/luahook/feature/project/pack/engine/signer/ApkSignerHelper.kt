package com.kulipai.luahook.feature.project.pack.engine.signer

import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * APK 签名配置参数
 */
data class SignConfig(
    val useCustomKeystore: Boolean = false,
    val keystoreFile: File? = null,
    val keystorePassword: String = "",
    val keyAlias: String = "",
    val keyPassword: String = "",
    val v1SigningEnabled: Boolean = true,
    val v2SigningEnabled: Boolean = true,
    val v3SigningEnabled: Boolean = true
)

/**
 * APK 数字签名与验签器（基于 Google 官方 `apksig` 引擎）。
 * 支持内置 TestKey 与自定义 Keystore（PKCS12 / JKS / BKS），支持 V1 / V2 / V3 签名组合。
 */
object ApkSignerHelper {

    fun signApk(inputApk: File, outputApk: File, config: SignConfig) {
        require(config.v1SigningEnabled || config.v2SigningEnabled || config.v3SigningEnabled) {
            "At least one signing scheme (V1, V2, or V3) must be enabled"
        }

        val (privateKey, certs, signerName) = if (config.useCustomKeystore) {
            loadCustomKeystore(config)
        } else {
            Triple(TestKeys.privateKey, listOf(TestKeys.certificate), "CERT")
        }

        val safeSignerName = signerName
            .uppercase()
            .replace(Regex("[^A-Z0-9_\\-]"), "_")
            .take(8)
            .ifBlank { "CERT" }

        val signerConfig = ApkSigner.SignerConfig.Builder(
            safeSignerName,
            privateKey,
            certs
        ).build()

        outputApk.parentFile?.mkdirs()
        if (outputApk.exists()) {
            outputApk.delete()
        }

        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(inputApk)
            .setOutputApk(outputApk)
            .setMinSdkVersion(21)
            .setV1SigningEnabled(config.v1SigningEnabled)
            .setV2SigningEnabled(config.v2SigningEnabled)
            .setV3SigningEnabled(config.v3SigningEnabled)
            .build()
            .sign()
    }

    fun verifyApk(apkFile: File): ApkVerifier.Result {
        return ApkVerifier.Builder(apkFile)
            .setMinCheckedPlatformVersion(21)
            .setMaxCheckedPlatformVersion(35)
            .build()
            .verify()
    }

    private fun loadCustomKeystore(config: SignConfig): Triple<PrivateKey, List<X509Certificate>, String> {
        val ksFile = config.keystoreFile
            ?: throw IllegalArgumentException("Keystore file is not specified")
        require(ksFile.exists() && ksFile.isFile) {
            "Keystore file does not exist: ${ksFile.absolutePath}"
        }

        val storePass = config.keystorePassword.toCharArray()
        val types = linkedSetOf("PKCS12", "JKS", "BKS", KeyStore.getDefaultType())
        var loadedKeyStore: KeyStore? = null
        var lastError: Throwable? = null

        for (type in types) {
            try {
                val ks = KeyStore.getInstance(type)
                FileInputStream(ksFile).use { fis ->
                    ks.load(fis, storePass)
                }
                loadedKeyStore = ks
                break
            } catch (t: Throwable) {
                lastError = t
            }
        }

        val ks = loadedKeyStore
            ?: throw IllegalArgumentException("Failed to load keystore (${lastError?.message ?: "unknown format or wrong password"})")

        val alias = if (config.keyAlias.isNotBlank()) {
            config.keyAlias.trim()
        } else {
            val aliases = ks.aliases()
            var found: String? = null
            while (aliases.hasMoreElements()) {
                val a = aliases.nextElement()
                if (ks.isKeyEntry(a)) {
                    found = a
                    break
                }
            }
            found ?: throw IllegalArgumentException("No private key entry found in keystore")
        }

        val keyPass = if (config.keyPassword.isNotBlank()) {
            config.keyPassword.toCharArray()
        } else {
            storePass
        }

        val key = ks.getKey(alias, keyPass) as? PrivateKey
            ?: throw IllegalArgumentException("Alias '$alias' does not contain a valid PrivateKey")

        val certChain = ks.getCertificateChain(alias)
            ?.mapNotNull { it as? X509Certificate }
            ?.takeIf { it.isNotEmpty() }
            ?: (ks.getCertificate(alias) as? X509Certificate)?.let { listOf(it) }
            ?: throw IllegalArgumentException("No X.509 certificate chain found for alias '$alias'")

        return Triple(key, certChain, alias)
    }
}
