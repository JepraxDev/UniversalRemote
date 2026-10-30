package com.universalremote.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStorageManager(context: Context) {

    private val prefsName = "universal_remote_secure_tokens"
    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private val alias = "UniversalRemoteKeyAlias"

    init {
        initKeyStore()
    }

    private fun initKeyStore() {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        if (!ks.containsAlias(alias)) {
            val kgen = KeyGenerator.getInstance("AES", "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            kgen.init(spec)
            kgen.generateKey()
        }
    }

    fun saveToken(deviceId: String, token: String) {
        val encrypted = encrypt(token)
        check(prefs.edit().putString(deviceId, encrypted).commit()) { "No se pudo guardar la credencial cifrada" }
    }

    fun getToken(deviceId: String): String? {
        val stored = prefs.getString(deviceId, null) ?: return null
        return try {
            decrypt(stored)
        } catch (_: Exception) {
            null
        }
    }

    fun clearToken(deviceId: String) {
        prefs.edit().remove(deviceId).apply()
    }

    private fun encrypt(plainText: String): String {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val secretKey = (ks.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedBase64: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val secretKey = (ks.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, combined, 0, 12)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = cipher.doFinal(combined, 12, combined.size - 12)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
