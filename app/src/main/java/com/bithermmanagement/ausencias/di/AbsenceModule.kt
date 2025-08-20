package com.bithermmanagement.ausencias.di

import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.ausencias.repository.AbsenceRepository
import com.bithermmanagement.ausencias.services.GoogleSheetsTransferService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AbsenceModule {
    
    @Provides
    @Singleton
    fun provideAbsenceRepository(
        database: AppDatabase,
        googleSheetsTransferService: GoogleSheetsTransferService
    ): AbsenceRepository {
        return AbsenceRepository(
            absenceRecordDao = database.absenceRecordDao(),
            absenceRequestDao = database.absenceRequestDao(),
            absenceLogDao = database.absenceLogDao(),
            googleSheetsTransferService = googleSheetsTransferService
        )
    }
}
