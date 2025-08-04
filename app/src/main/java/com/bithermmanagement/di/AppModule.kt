package com.bithermmanagement.di

import android.content.Context
import com.bithermmanagement.utils.GoogleDriveHelper
import com.bithermmanagement.utils.GoogleSheetsHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGoogleSheetsHelper(@ApplicationContext context: Context): GoogleSheetsHelper {
        return GoogleSheetsHelper(context)
    }

    @Provides
    @Singleton
    fun provideGoogleDriveHelper(@ApplicationContext context: Context): GoogleDriveHelper {
        return GoogleDriveHelper(context)
    }
} 