package com.bithermmanagement.ausencias.di

import com.bithermmanagement.ausencias.dao.AbsenceRecordDao
import com.bithermmanagement.ausencias.dao.AbsenceRequestDao
import com.bithermmanagement.ausencias.dao.AbsenceLogDao
import com.bithermmanagement.ausencias.repository.AbsenceRepository
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
        absenceRecordDao: AbsenceRecordDao,
        absenceRequestDao: AbsenceRequestDao,
        absenceLogDao: AbsenceLogDao
    ): AbsenceRepository {
        return AbsenceRepository(absenceRecordDao, absenceRequestDao, absenceLogDao)
    }
}
