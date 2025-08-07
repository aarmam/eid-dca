package com.github.aarmam.eid.dca.service


import android.app.Activity
import java.security.cert.X509Certificate

interface IdCardService {

    suspend fun getCertificates(activity: Activity, canNumber: String): Pair<X509Certificate, X509Certificate>
    suspend fun signPin1(activity: Activity, canNumber: String, pin1: String, dataToSign: ByteArray): ByteArray
    suspend fun signPin2(activity: Activity, canNumber: String, pin2: String, dataToSign: ByteArray): ByteArray

}
