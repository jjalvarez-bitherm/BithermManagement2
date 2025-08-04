package com.bithermmanagement.core.di

import android.content.Context
import androidx.room.Room
import com.bithermmanagement.core.data.db.AppDatabase
import com.bithermmanagement.core.data.db.MenuDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "bitherm_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideMenuDao(appDatabase: AppDatabase): MenuDao {
        return appDatabase.menuDao()
    }
} 