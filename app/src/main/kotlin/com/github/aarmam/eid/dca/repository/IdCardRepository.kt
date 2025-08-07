package com.github.aarmam.eid.dca.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "id_card_prefs")

@Singleton
class IdCardRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val CAN_NUMBER_KEY = stringPreferencesKey("can_number")
        private val SURNAME_KEY = stringPreferencesKey("surname")
        private val GIVEN_NAME_KEY = stringPreferencesKey("given_name")
        private val SERIAL_NUMBER_KEY = stringPreferencesKey("serial_number")
        private val AUTH_CREDENTIAL_KEY = stringPreferencesKey("auth_credential")
        private val AUTH_CERT_KEY = stringPreferencesKey("auth_cert")
        private val SIGN_CERT_KEY = stringPreferencesKey("sign_cert")
    }

    suspend fun saveIdCardData(
        canNumber: String,
        surname: String,
        givenName: String,
        serialNumber: String,
        authenticationCredential: String,
        authCert: X509Certificate,
        signCert: X509Certificate
    ) {
        context.dataStore.edit { preferences ->
            preferences[CAN_NUMBER_KEY] = canNumber
            preferences[SURNAME_KEY] = surname
            preferences[GIVEN_NAME_KEY] = givenName
            preferences[SERIAL_NUMBER_KEY] = serialNumber
            preferences[AUTH_CREDENTIAL_KEY] = authenticationCredential
            preferences[AUTH_CERT_KEY] = Base64.getEncoder().encodeToString(authCert.encoded)
            preferences[SIGN_CERT_KEY] = Base64.getEncoder().encodeToString(signCert.encoded)
        }
    }

    val idCardData: Flow<IdCardData?> = context.dataStore.data.map { preferences ->
        val surname = preferences[SURNAME_KEY]
        val givenName = preferences[GIVEN_NAME_KEY]
        val serialNumber = preferences[SERIAL_NUMBER_KEY]

        if (surname != null && givenName != null && serialNumber != null) {
            IdCardData(surname, givenName, serialNumber)
        } else {
            null
        }
    }

    suspend fun getCanNumber(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[CAN_NUMBER_KEY]
        }.first()
    }

    suspend fun getAuthenticationCredential(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[AUTH_CREDENTIAL_KEY]
        }.first()
    }

    suspend fun getAuthCert(): X509Certificate? {
        return context.dataStore.data.map { preferences ->
            preferences[AUTH_CERT_KEY]?.let { base64Cert ->
                try {
                    val certBytes = Base64.getDecoder().decode(base64Cert)
                    val certFactory = CertificateFactory.getInstance("X.509")
                    val inputStream = ByteArrayInputStream(certBytes)
                    certFactory.generateCertificate(inputStream) as X509Certificate
                } catch (e: Exception) {
                    null
                }
            }
        }.first()
    }

    suspend fun getSignCert(): X509Certificate? {
        return context.dataStore.data.map { preferences ->
            preferences[SIGN_CERT_KEY]?.let { base64Cert ->
                try {
                    val certBytes = Base64.getDecoder().decode(base64Cert)
                    val certFactory = CertificateFactory.getInstance("X.509")
                    val inputStream = ByteArrayInputStream(certBytes)
                    certFactory.generateCertificate(inputStream) as X509Certificate
                } catch (e: Exception) {
                    null
                }
            }
        }.first()
    }

    suspend fun clearIdCardData() {
        context.dataStore.edit { preferences ->
            preferences.remove(SURNAME_KEY)
            preferences.remove(GIVEN_NAME_KEY)
            preferences.remove(SERIAL_NUMBER_KEY)
            preferences.remove(AUTH_CREDENTIAL_KEY)
            preferences.remove(AUTH_CERT_KEY)
            preferences.remove(SIGN_CERT_KEY)
            preferences.remove(CAN_NUMBER_KEY)
        }
    }
}

data class IdCardData(
    val surname: String,
    val givenName: String,
    val serialNumber: String
)