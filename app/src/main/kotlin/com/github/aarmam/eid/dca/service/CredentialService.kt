package com.github.aarmam.eid.dca.service

import android.app.Activity
import id.walt.mdoc.doc.MDoc
import java.security.cert.X509Certificate

interface CredentialService {
    suspend fun createAuthenticationCredential(
        activity: Activity,
        canNumber: String,
        pin2: String,
        authCert: X509Certificate,
        signCert: X509Certificate
    ): MDoc

    suspend fun createVpToken(
        activity: Activity,
        pin1: String,
        origin: String,
        nonce: String,
        canNumber: String,
        authCert: X509Certificate
    ): String

}