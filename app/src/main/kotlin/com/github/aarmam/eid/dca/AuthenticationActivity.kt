package com.github.aarmam.eid.dca

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.registry.provider.selectedEntryId
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.github.aarmam.eid.dca.repository.IdCardRepository
import com.github.aarmam.eid.dca.service.CredentialService
import com.github.aarmam.eid.dca.ui.AuthenticationScreen
import com.github.aarmam.eid.dca.ui.theme.EIDWithDigitalCredentialsAPITheme
import dagger.hilt.android.AndroidEntryPoint
import ee.ria.DigiDoc.idcard.CodeVerificationException
import ee.ria.DigiDoc.idcard.PaceTunnelException
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class AuthenticationActivity : FragmentActivity() {

    @Inject
    lateinit var credentialService: CredentialService

    @Inject
    lateinit var idCardRepository: IdCardRepository

    @Inject
    lateinit var allowedAppsJson: String

    companion object {
        private const val TAG = "GetCredentialActivity"
    }

    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)

    @OptIn(ExperimentalDigitalCredentialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        if (request != null) {
            Log.i(TAG, "Selected entry ID: ${request.selectedEntryId}")
            val digitalCredentialOption = request.credentialOptions.filterIsInstance<GetDigitalCredentialOption>()
                .firstOrNull()
            if (digitalCredentialOption == null) {
                throw IllegalArgumentException("No digital credential option found")
            }
            val origin = request.callingAppInfo.getOrigin(allowedAppsJson)!!
            val nonce = extractNonce(digitalCredentialOption.requestJson)
            Log.i(TAG, "Processing request: ${digitalCredentialOption.requestJson}")

            setContent {
                EIDWithDigitalCredentialsAPITheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AuthenticationScreen(
                            origin = origin,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            onAuthenticate = { pin1 ->
                                handleCredentialRequest(pin1, origin, nonce)
                            }
                        )
                    }
                }
            }
        } else {
            Log.w(TAG, "No credential request found")
            finish()
        }
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    private fun handleCredentialRequest(pin1: String, origin: String, nonce: String) {
        val resultData = Intent()

        lifecycleScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val canNumber = idCardRepository.getCanNumber()
                val authCert = idCardRepository.getAuthCert()
                val vpToken = credentialService.createVpToken(
                    this@AuthenticationActivity,
                    pin1,
                    origin,
                    nonce,
                    canNumber!!,
                    authCert!!
                )
                val responseJson = JSONObject().put("vp_token", vpToken).toString()
                val digitalCredential = DigitalCredential(responseJson)
                val credentialResponse = GetCredentialResponse(digitalCredential)
                PendingIntentHandler.setGetCredentialResponse(resultData, credentialResponse)
                setResult(RESULT_OK, resultData)
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing credential request", e)
                errorMessage = when (e) {
                    is PaceTunnelException ->
                        "Invalid CAN number: Please check the 6-digit number on your ID card"

                    is CodeVerificationException ->
                        e.message ?: "Code verification failed"

                    else -> "Unknown exception. Check logs."
                }
                isLoading = false
            } finally {
                isLoading = false
            }
        }
    }

    private fun extractNonce(requestJson: String): String {
        val requestJsonObject = JSONObject(requestJson)
        val requestsArray = requestJsonObject.getJSONArray("requests")
        if (requestsArray.length() > 0) {
            val firstRequest = requestsArray.getJSONObject(0)
            val dataObject = firstRequest.getJSONObject("data")
            return dataObject.getString("nonce")
        } else {
            throw IllegalArgumentException("No requests found in requestJson")
        }
    }
}
