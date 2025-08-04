package com.bithermmanagement.database.di

import android.content.Context
import androidx.room.Room
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.dao.EquipoDao
import com.bithermmanagement.database.dao.InspeccionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "bitherm_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideEquipoDao(db: AppDatabase): EquipoDao = db.equipoDao()

    @Provides
    fun provideInspeccionDao(db: AppDatabase): InspeccionDao = db.inspeccionDao()
} 