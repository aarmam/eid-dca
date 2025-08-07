package com.github.aarmam.eid.dca.di

import android.content.Context
import com.github.aarmam.eid.dca.repository.IdCardRepository
import com.github.aarmam.eid.dca.service.CredentialService
import com.github.aarmam.eid.dca.service.CredentialServiceImpl
import com.github.aarmam.eid.dca.service.IdCardService
import com.github.aarmam.eid.dca.service.IdCardServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ee.ria.DigiDoc.smartcardreader.nfc.NfcSmartCardReaderManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModules {

    @Provides
    fun provideNfcSmartCardReaderManager(): NfcSmartCardReaderManager = NfcSmartCardReaderManager()

    @Provides
    fun provideIdCardService(
        nfcSmartCardReaderManager: NfcSmartCardReaderManager
    ): IdCardService = IdCardServiceImpl(nfcSmartCardReaderManager)

    @Provides
    fun provideCredentialService(
        idCardService: IdCardService,
        idCardRepository: IdCardRepository
    ): CredentialService = CredentialServiceImpl(idCardService, idCardRepository)

    @Provides
    @Singleton
    fun provideIdCardRepository(@ApplicationContext context: Context): IdCardRepository {
        return IdCardRepository(context)
    }

    @Provides
    @Singleton
    fun provideAllowedAppsJson(@ApplicationContext context: Context): String {
        return context.assets.open("apps.json").bufferedReader().use { it.readText() }
    }
}