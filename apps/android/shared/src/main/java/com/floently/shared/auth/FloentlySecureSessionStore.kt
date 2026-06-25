package com.floently.shared.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class FloentlySecureSessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("floently_native_auth_secure", Context.MODE_PRIVATE)
    private val keyAlias = "floently_native_auth_session_key"
    private val keyStoreType = "AndroidKeyStore"
    private val cipherName = "AES/GCM/NoPadding"

    var session: FloentlyAuthSession? = load()
        private set

    fun save(session: FloentlyAuthSession) {
        val encrypted = encrypt(session.toJson().toString().toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString("session_ciphertext", encrypted.ciphertext)
            .putString("session_iv", encrypted.iv)
            .apply()

        this.session = session
    }

    fun clear() {
        session = null
        prefs.edit()
            .remove("session_ciphertext")
            .remove("session_iv")
            .apply()
    }

    private fun load(): FloentlyAuthSession? {
        val ciphertext = prefs.getString("session_ciphertext", null) ?: return null
        val iv = prefs.getString("session_iv", null) ?: return null

        return runCatching {
            val decrypted = decrypt(EncryptedPayload(ciphertext = ciphertext, iv = iv))
            authSessionFromJson(JSONObject(String(decrypted, Charsets.UTF_8)))
        }.getOrNull()
    }

    private fun encrypt(bytes: ByteArray): EncryptedPayload {
        val cipher = Cipher.getInstance(cipherName)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val ciphertext = cipher.doFinal(bytes)

        return EncryptedPayload(
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        )
    }

    private fun decrypt(payload: EncryptedPayload): ByteArray {
        val cipher = Cipher.getInstance(cipherName)
        val iv = Base64.decode(payload.iv, Base64.NO_WRAP)
        val ciphertext = Base64.decode(payload.ciphertext, Base64.NO_WRAP)

        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))

        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(keyStoreType).apply { load(null) }

        val existing = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        if (existing != null) {
            return existing.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreType)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private data class EncryptedPayload(
        val ciphertext: String,
        val iv: String
    )
}
