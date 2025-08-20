package com.bithermmanagement.di

import android.content.Context
import com.bithermmanagement.utils.GoogleDriveHelper
import com.bithermmanagement.utils.GoogleSheetsHelper
import com.bithermmanagement.data.UserManager
import com.bithermmanagement.ausencias.services.GoogleSheetsTransferService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.bithermmanagement.database.dao.UserDao
import com.bithermmanagement.data.GoogleSheetsManager
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.data.GoogleAuthAdapter

@Module(includes = [
    com.bithermmanagement.ausencias.di.AbsenceModule::class,
    com.bithermmanagement.core.di.DatabaseModule::class
])
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    @Singleton
    fun provideGoogleSheetsHelper(@ApplicationContext context: Context): GoogleSheetsHelper {
        return GoogleSheetsHelper(context)
    }
    
    @Provides
    @Singleton
    fun provideGoogleAuthAdapter(@ApplicationContext context: Context): GoogleAuthAdapter {
        // Usar credenciales de servicio por defecto desde assets
        val credentialsStream = context.assets.open("credentials_default.json")
        return GoogleAuthAdapter(
            context = context,
            useOAuth = false,
            oAuthEmail = "",
            credentialsStream = credentialsStream
        )
    }
    
    @Provides
    @Singleton
    fun provideGoogleSheetsManager(
        authAdapter: GoogleAuthAdapter,
        @ApplicationContext context: Context
    ): GoogleSheetsManager {
        return GoogleSheetsManager(authAdapter, context)
    }
    
    @Provides
    @Singleton
    fun provideUserManager(
        userDao: UserDao,
        @ApplicationContext context: Context
    ): UserManager {
        return UserManager(userDao, context)
    }
    
    @Provides
    @Singleton
    fun provideGoogleSheetsTransferService(
        @ApplicationContext context: Context
    ): GoogleSheetsTransferService {
        return GoogleSheetsTransferService(context)
    }
} 