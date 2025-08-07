package com.github.aarmam.eid.dca.service


import android.app.Activity
import android.util.Log
import ee.ria.DigiDoc.idcard.CertificateType
import ee.ria.DigiDoc.idcard.TokenWithPace
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderException
import ee.ria.DigiDoc.smartcardreader.nfc.NfcSmartCardReaderManager
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bouncycastle.util.encoders.Hex
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class IdCardServiceImpl
@Inject
constructor(
    private val nfcSmartCardReaderManager: NfcSmartCardReaderManager
) : IdCardService {

    companion object {
        private const val TAG = "IdCardServiceImpl"
    }

    override suspend fun getCertificates(
        activity: Activity,
        canNumber: String
    ): Pair<X509Certificate, X509Certificate> =
        suspendCancellableCoroutine { continuation ->
            nfcSmartCardReaderManager.startDiscovery(activity) { nfcReader, exc ->
                if ((nfcReader != null) && (exc == null)) {
                    try {
                        // Create card session over NFC
                        val card = TokenWithPace.create(nfcReader)
                        // Establish PACE tunnel with previously captured CAN
                        card.tunnel(canNumber)
                        val authCert = card.certificate(CertificateType.AUTHENTICATION)
                        val signCert = card.certificate(CertificateType.SIGNING)
                        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
                        val authInps = ByteArrayInputStream(authCert)
                        val authX509Certificate: X509Certificate = cf.generateCertificate(authInps) as X509Certificate
                        val signInps = ByteArrayInputStream(signCert)
                        val signX509Certificate: X509Certificate = cf.generateCertificate(signInps) as X509Certificate
                        val result = Pair(authX509Certificate, signX509Certificate)

                        continuation.resume(result)
                    } catch (ex: SmartCardReaderException) {
                        Log.e(TAG, "Error during NFC authentication", ex)
                        continuation.resumeWithException(ex)
                    } finally {
                        nfcSmartCardReaderManager.disableNfcReaderMode()
                    }
                } else {
                    continuation.resumeWithException(
                        exc ?: Exception("NFC reader not available")
                    )
                }
            }

            continuation.invokeOnCancellation {
                nfcSmartCardReaderManager.disableNfcReaderMode()
            }
        }

    override suspend fun signPin1(
        activity: Activity,
        canNumber: String,
        pin1: String,
        dataToSign: ByteArray
    ): ByteArray =
        suspendCancellableCoroutine { continuation ->
            nfcSmartCardReaderManager.startDiscovery(activity) { nfcReader, exc ->
                if ((nfcReader != null) && (exc == null)) {
                    try {
                        val card = TokenWithPace.create(nfcReader)
                        card.tunnel(canNumber)
                        val dataToSignHash = MessageDigest.getInstance("SHA-384").digest(dataToSign)
                        val signatureArray = card.authenticate(pin1.toByteArray(), dataToSignHash)
                        Log.i(TAG, "SIGNATURE ${Hex.toHexString(signatureArray)}")
                        continuation.resume(signatureArray)
                    } catch (ex: SmartCardReaderException) {
                        Log.e(TAG, "Error during NFC authentication", ex)
                        continuation.resumeWithException(ex)
                    } finally {
                        nfcSmartCardReaderManager.disableNfcReaderMode()
                    }
                } else {
                    continuation.resumeWithException(
                        exc ?: Exception("NFC reader not available")
                    )
                }
            }

            continuation.invokeOnCancellation {
                nfcSmartCardReaderManager.disableNfcReaderMode()
            }
        }

    override suspend fun signPin2(
        activity: Activity,
        canNumber: String,
        pin2: String,
        dataToSign: ByteArray
    ): ByteArray =
        suspendCancellableCoroutine { continuation ->
            nfcSmartCardReaderManager.startDiscovery(activity) { nfcReader, exc ->
                if ((nfcReader != null) && (exc == null)) {
                    try {
                        val card = TokenWithPace.create(nfcReader)
                        card.tunnel(canNumber)
                        val dataToSignHash = MessageDigest.getInstance("SHA-384").digest(dataToSign)
                        val signatureArray = card.calculateSignature(pin2.toByteArray(), dataToSignHash, true)
                        Log.i(TAG, "SIGNATURE ${Hex.toHexString(signatureArray)}")

                        continuation.resume(signatureArray)
                    } catch (ex: SmartCardReaderException) {
                        Log.e(TAG, "Error during NFC signing", ex)
                        continuation.resumeWithException(ex)
                    } finally {
                        nfcSmartCardReaderManager.disableNfcReaderMode()
                    }
                } else {
                    continuation.resumeWithException(
                        exc ?: Exception("NFC reader not available")
                    )
                }
            }

            continuation.invokeOnCancellation {
                nfcSmartCardReaderManager.disableNfcReaderMode()
            }
        }
}
