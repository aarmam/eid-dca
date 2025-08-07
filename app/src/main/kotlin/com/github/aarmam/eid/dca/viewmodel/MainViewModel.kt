package com.github.aarmam.eid.dca.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.aarmam.eid.dca.repository.IdCardRepository
import com.github.aarmam.eid.dca.service.CredentialService
import com.github.aarmam.eid.dca.service.IdCardService
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.ria.DigiDoc.idcard.CodeVerificationException
import ee.ria.DigiDoc.idcard.PaceTunnelException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import java.security.cert.CertificateEncodingException
import java.util.Arrays
import java.util.stream.Collectors
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val idCardService: IdCardService,
    private val credentialService: CredentialService,
    private val idCardRepository: IdCardRepository
) : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadSavedIdCardData()
    }

    private fun loadSavedIdCardData() {
        viewModelScope.launch {
            idCardRepository.idCardData.collect { idCardData ->
                if (idCardData != null) {
                    _uiState.value = _uiState.value.copy(
                        surname = idCardData.surname,
                        givenName = idCardData.givenName,
                        serialNumber = idCardData.serialNumber
                    )
                }
            }
        }
    }

    fun registerIdCard(activity: Activity, canNumber: String, pin2: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                Log.d(TAG, "Registering ID card with CAN: $canNumber")
                val (authCert, signCert) = idCardService.getCertificates(activity, canNumber)
                val authenticationCredential =
                    credentialService.createAuthenticationCredential(activity, canNumber, pin2, authCert, signCert)
                val signCertHolder = JcaX509CertificateHolder(signCert)
                val surname = getField(signCertHolder.subject, BCStyle.SURNAME)
                val givenName = getField(signCertHolder.subject, BCStyle.GIVENNAME)
                val serialNumber = getField(signCertHolder.subject, BCStyle.SERIALNUMBER)

                Log.d(
                    TAG,
                    "Generated authentication credential in CBOR encoded MDOC: ${authenticationCredential.toCBORHex()}"
                )

                idCardRepository.saveIdCardData(
                    canNumber = canNumber,
                    surname = surname!!,
                    givenName = givenName!!,
                    serialNumber = serialNumber!!,
                    authenticationCredential = authenticationCredential.toCBORHex(),
                    authCert = authCert,
                    signCert = signCert
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    registrationSuccess = true,
                    surname = surname,
                    givenName = givenName,
                    serialNumber = serialNumber
                )
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is PaceTunnelException ->
                        "Invalid CAN number: Please check the 6-digit number on your ID card"

                    is CodeVerificationException ->
                        e.message ?: "Code verification failed"

                    else -> "Unknown exception. Check logs."
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    fun getField(x500Name: X500Name, fieldId: ASN1ObjectIdentifier?): String? {
        val rdns = x500Name.getRDNs(fieldId)
        if (rdns.size != 0 && rdns[0]!!.getFirst() != null) {
            return Arrays.stream(rdns).map({ rdn -> IETFUtils.valueToString(rdn.getFirst().value) })
                .collect(Collectors.joining(", "))
        } else {
            throw CertificateEncodingException("X500 name RDNs empty or first element is null")
        }
    }

    fun removeIdCard() {
        viewModelScope.launch {
            try {
                idCardRepository.clearIdCardData()
                _uiState.value = _uiState.value.copy(
                    surname = null,
                    givenName = null,
                    serialNumber = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error removing ID card", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val registrationSuccess: Boolean = false,
    val error: String? = null,
    val surname: String? = null,
    val givenName: String? = null,
    val serialNumber: String? = null
)