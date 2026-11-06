package com.example.moodwave.data.preferences

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.securityDataStore by preferencesDataStore(
    name = "moodwave_security"
)

class SecurityPreferences(
    private val context: Context
) {

    companion object {

        private val PIN_HASH =
            stringPreferencesKey("pin_hash")

        private val PIN_SALT =
            stringPreferencesKey("pin_salt")

        private val BIOMETRIC_ENABLED =
            booleanPreferencesKey("biometric_enabled")

        private val AUTO_LOCK_SECONDS =
            intPreferencesKey("auto_lock_seconds")
    }

    val pinConfigurado: Flow<Boolean> =
        context.securityDataStore.data.map { preferences ->

            !preferences[PIN_HASH].isNullOrBlank() &&
                    !preferences[PIN_SALT].isNullOrBlank()
        }

    val biometriaActivada: Flow<Boolean> =
        context.securityDataStore.data.map { preferences ->

            preferences[BIOMETRIC_ENABLED] ?: true
        }

    val bloqueoAutomaticoSegundos: Flow<Int> =
        context.securityDataStore.data.map { preferences ->

            preferences[AUTO_LOCK_SECONDS] ?: 60
        }

    suspend fun guardarPin(
        pin: String
    ) {

        val salt = generarSalt()
        val hash = generarHash(pin, salt)

        context.securityDataStore.edit { preferences ->

            preferences[PIN_SALT] = salt
            preferences[PIN_HASH] = hash
        }
    }

    suspend fun validarPin(
        pin: String
    ): Boolean {

        val preferences =
            context.securityDataStore.data.first()

        val salt =
            preferences[PIN_SALT]
                ?: return false

        val hashGuardado =
            preferences[PIN_HASH]
                ?: return false

        val hashIngresado =
            generarHash(
                pin,
                salt
            )

        return MessageDigest.isEqual(
            Base64.decode(
                hashGuardado,
                Base64.NO_WRAP
            ),
            Base64.decode(
                hashIngresado,
                Base64.NO_WRAP
            )
        )
    }

    suspend fun cambiarBiometria(
        activada: Boolean
    ) {

        context.securityDataStore.edit { preferences ->

            preferences[BIOMETRIC_ENABLED] =
                activada
        }
    }

    suspend fun cambiarBloqueoAutomatico(
        segundos: Int
    ) {

        context.securityDataStore.edit { preferences ->

            preferences[AUTO_LOCK_SECONDS] =
                segundos
        }
    }

    private fun generarSalt(): String {

        val bytes =
            ByteArray(16)

        SecureRandom()
            .nextBytes(bytes)

        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP
        )
    }

    private fun generarHash(
        pin: String,
        saltBase64: String
    ): String {

        val salt =
            Base64.decode(
                saltBase64,
                Base64.NO_WRAP
            )

        val spec =
            PBEKeySpec(
                pin.toCharArray(),
                salt,
                100000,
                256
            )

        val factory =
            SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA1"
            )

        val hash =
            factory
                .generateSecret(spec)
                .encoded

        spec.clearPassword()

        return Base64.encodeToString(
            hash,
            Base64.NO_WRAP
        )
    }
}