package com.github.aarmam.eid.dca.service

import android.app.Activity
import com.github.aarmam.eid.dca.repository.IdCardRepository
import id.walt.mdoc.cose.COSESign1
import id.walt.mdoc.dataelement.ByteStringElement
import id.walt.mdoc.dataelement.DataElement
import id.walt.mdoc.dataelement.EncodedCBORElement
import id.walt.mdoc.dataelement.ListElement
import id.walt.mdoc.dataelement.MapElement
import id.walt.mdoc.dataelement.MapKey
import id.walt.mdoc.dataelement.NullElement
import id.walt.mdoc.dataelement.NumberElement
import id.walt.mdoc.dataelement.StringElement
import id.walt.mdoc.dataretrieval.DeviceResponse
import id.walt.mdoc.devicesigned.DeviceAuth
import id.walt.mdoc.devicesigned.DeviceSigned
import id.walt.mdoc.doc.MDoc
import id.walt.mdoc.doc.MDocBuilder
import id.walt.mdoc.docrequest.MDocRequest
import id.walt.mdoc.docrequest.MDocRequestBuilder
import id.walt.mdoc.issuersigned.IssuerSigned
import id.walt.mdoc.issuersigned.IssuerSignedItem
import id.walt.mdoc.mdocauth.DeviceAuthentication
import id.walt.mdoc.mso.DeviceKeyInfo
import id.walt.mdoc.mso.MSO
import id.walt.mdoc.mso.ValidityInfo
import kotlinx.datetime.Instant
import org.cose.java.OneKey
import org.kotlincrypto.hash.sha2.SHA256
import java.security.cert.X509Certificate
import javax.inject.Inject

private const val DOC_TYPE = "eu.europa.ec.eudi.eid.1"

class CredentialServiceImpl @Inject constructor(
    private val idCardService: IdCardService,
    private val idCardRepository: IdCardRepository
) : CredentialService {

    override suspend fun createAuthenticationCredential(
        activity: Activity,
        canNumber: String,
        pin2: String,
        authCert: X509Certificate,
        signCert: X509Certificate
    ): MDoc {
        val mdoc = MDocBuilder(DOC_TYPE)
        val validFrom = Instant.fromEpochSeconds(1671581600L)
        val validUntil = Instant.fromEpochSeconds(1771581600L)
        val validityInfo = ValidityInfo(validFrom, validFrom, validUntil)
        val deviceKeyInfo = DeviceKeyInfo(
            DataElement.fromCBOR(
                OneKey(authCert.publicKey, null).AsCBOR().EncodeToBytes()
            )
        )
        val mso = MSO.createFor(mdoc.nameSpacesMap, deviceKeyInfo, mdoc.docType, validityInfo)
        val payload = getCoseSign1Payload(mso.toMapElement().toEncodedCBORElement().toCBOR())
        val signature = idCardService.signPin2(activity, canNumber, pin2, payload.toCBOR())
        val issuerAuth = getIssuerAuth(signCert, mso, signature)
        return mdoc.build(issuerAuth)
    }

    override suspend fun createVpToken(
        activity: Activity,
        pin1: String,
        origin: String,
        nonce: String,
        canNumber: String,
        authCert: X509Certificate
    ): String {
        val eidAuthMDocWithDeviceSignature = presentWithDeviceSignature(
            activity, pin1, origin, nonce, canNumber, authCert
        )
        val deviceResponse = DeviceResponse(listOf(eidAuthMDocWithDeviceSignature)).toCBORBase64URL()
        return deviceResponse
    }

    private fun getIssuerAuth(
        signCert: X509Certificate,
        mso: MSO,
        signature: ByteArray
    ): COSESign1 {
        val data = listOf(
            ByteStringElement(MapElement(mapOf(MapKey(1) to NumberElement(-35))).toCBOR()),
            MapElement(mapOf(MapKey(33) to ByteStringElement(signCert.encoded))),
            ByteStringElement(mso.toMapElement().toEncodedCBORElement().toCBOR()),
            ByteStringElement(signature)
        )
        val issuerAuth = COSESign1(data)
        return issuerAuth
    }

    private suspend fun presentWithDeviceSignature(
        activity: Activity,
        pin1: String,
        origin: String,
        nonce: String,
        canNumber: String,
        authCert: X509Certificate
    ): MDoc {
        val authenticationCredential = idCardRepository.getAuthenticationCredential()
        if (authenticationCredential == null) {
            throw IllegalStateException("Authentication credential not found. Please create authentication credential first.")
        }
        val mdoc = MDoc.fromCBORHex(authenticationCredential)
        val mDocRequest = MDocRequestBuilder(DOC_TYPE).build(null)
        val handover =
            ListElement(listOf(StringElement(origin), StringElement(nonce), NullElement()))
        val sessionTranscript = generateSessionTranscript(handover)
        val deviceNameSpaces = EncodedCBORElement(MapElement(mapOf()))
        val deviceAuthentication =
            DeviceAuthentication(sessionTranscript, DOC_TYPE, deviceNameSpaces)
        val deviceAuthenticationPayload = getDeviceSignedPayload(deviceAuthentication)
        val payload = getCoseSign1Payload(deviceAuthenticationPayload)
        val signature = idCardService.signPin1(activity, canNumber, pin1, payload.toCBOR())
        val data = listOf(
            ByteStringElement(MapElement(mapOf(MapKey(1) to NumberElement(-35))).toCBOR()),
            MapElement(mapOf(MapKey(33) to ByteStringElement(authCert.encoded))),
            NullElement(), // Detached payload as required by ISO/IEC 18013-5
            ByteStringElement(signature)
        )
        val deviceSignature = COSESign1(data)
        return MDoc(
            StringElement(DOC_TYPE),
            selectDisclosures(mdoc.issuerSigned, mDocRequest),
            DeviceSigned(EncodedCBORElement(MapElement(mapOf())), DeviceAuth(deviceSignature = deviceSignature))
        )
    }

    private fun getCoseSign1Payload(payload: ByteArray): ListElement = ListElement(
        listOf(
            StringElement("Signature1"),
            ByteStringElement(MapElement(mapOf(MapKey(1) to NumberElement(-35))).toCBOR()),
            ByteStringElement("".toByteArray()),
            ByteStringElement(payload)
        )
    )

    private fun generateSessionTranscript(handover: ListElement): ListElement {
        val sessionTranscript = ListElement(
            listOf(
                NullElement(), NullElement(), ListElement(
                    listOf(
                        StringElement("OpenID4VPDCAPIHandover"),
                        ByteStringElement(SHA256().digest(handover.toCBOR())),
                    )
                )
            )
        )
        return sessionTranscript
    }

    private fun getDeviceSignedPayload(deviceAuthentication: DeviceAuthentication) =
        EncodedCBORElement(deviceAuthentication.toDE()).toCBOR()

    private fun selectDisclosures(issuerSigned: IssuerSigned, mDocRequest: MDocRequest): IssuerSigned {
        return IssuerSigned(
            issuerSigned.nameSpaces?.mapValues { entry ->
                val requestedItems = mDocRequest.getRequestedItemsFor(entry.key)
                entry.value.filter { encodedItem ->
                    requestedItems.containsKey(encodedItem.decode<IssuerSignedItem>().elementIdentifier.value)
                }
            },
            issuerSigned.issuerAuth
        )
    }
}