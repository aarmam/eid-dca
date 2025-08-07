package com.github.aarmam.eid.dca

import android.app.Application
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.registry.provider.RegisterCredentialsRequest
import androidx.credentials.registry.provider.RegistryManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.Security

@HiltAndroidApp
class EidApplication : Application() {
    private lateinit var registryManager: RegistryManager
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        setupBouncyCastle()
        setupCredentialManager()
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    private fun setupCredentialManager() {
        registryManager = RegistryManager.create(this)

        val credentialDatabase = createCredentialDatabase()
        val openId4VPDraft24Matcher = readAsset("openid4vp.wasm")
        val openId4VP1_0Matcher = readAsset("openid4vp1_0.wasm")

        applicationScope.launch {
            // Oid4vp draft 24
            // For backward compatibility with Chrome
            registryManager.registerCredentials(
                request = object : RegisterCredentialsRequest(
                    "com.credman.IdentityCredential",
                    "openid4vp",
                    credentialDatabase,
                    openId4VPDraft24Matcher
                ) {}
            )
            // In the future, should only register this type
            registryManager.registerCredentials(
                request = object : RegisterCredentialsRequest(
                    DigitalCredential.TYPE_DIGITAL_CREDENTIAL,
                    "openid4vp",
                    credentialDatabase,
                    openId4VPDraft24Matcher
                ) {}
            )

            // Oid4vp 1.0 Candidate
            // For backward compatibility with Chrome
            registryManager.registerCredentials(
                request = object : RegisterCredentialsRequest(
                    "com.credman.IdentityCredential",
                    "openid4vp1.0",
                    credentialDatabase,
                    openId4VP1_0Matcher
                ) {}
            )
            // In the future, should only register this type
            registryManager.registerCredentials(
                request = object : RegisterCredentialsRequest(
                    DigitalCredential.TYPE_DIGITAL_CREDENTIAL,
                    "openid4vp1.0",
                    credentialDatabase,
                    openId4VP1_0Matcher
                ) {}
            )
        }
    }

    private fun setupBouncyCastle() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())
    }

    private fun createCredentialDatabase(): ByteArray {
        val jsonBytes = readAsset("credentials.json")
        val buffer = ByteBuffer.allocate(4 + jsonBytes.size)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(4)
        buffer.put(jsonBytes)
        return buffer.array()
    }

    private fun readAsset(fileName: String): ByteArray {
        val stream = assets.open(fileName)
        val data = ByteArray(stream.available())
        stream.read(data)
        stream.close()
        return data
    }
}