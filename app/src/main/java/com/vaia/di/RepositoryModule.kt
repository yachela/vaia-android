package com.vaia.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vaia.data.api.CurrencyApiService
import com.vaia.data.api.VaiaApiService
import com.vaia.data.local.db.ActivityDao
import com.vaia.data.local.db.DocumentDao
import com.vaia.data.local.db.PackingDao
import com.vaia.data.local.db.TripDao
import com.vaia.data.local.db.VaiaDatabase
import com.vaia.data.network.ConnectivityObserver
import com.vaia.data.repository.ActivityRepositoryImpl
import com.vaia.data.repository.AuthRepositoryImpl
import com.vaia.data.repository.CurrencyRepositoryImpl
import com.vaia.data.repository.DocumentRepositoryImpl
import com.vaia.data.repository.ExpenseRepositoryImpl
import com.vaia.data.repository.PackingRepositoryImpl
import com.vaia.data.repository.TripRepositoryImpl
import com.vaia.data.sync.SyncManager
import com.vaia.domain.repository.ActivityRepository
import com.vaia.domain.repository.AuthRepository
import com.vaia.domain.repository.CurrencyRepository
import com.vaia.domain.repository.DocumentRepository
import com.vaia.domain.repository.ExpenseRepository
import com.vaia.domain.repository.PackingRepository
import com.vaia.domain.repository.TripRepository
import com.vaia.worker.ReminderScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideVaiaDatabase(@ApplicationContext context: Context): VaiaDatabase {
        return VaiaDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideTripDao(database: VaiaDatabase): TripDao {
        return database.tripDao()
    }

    @Provides
    @Singleton
    fun provideActivityDao(database: VaiaDatabase): ActivityDao {
        return database.activityDao()
    }

    @Provides
    @Singleton
    fun providePackingDao(database: VaiaDatabase): PackingDao {
        return database.packingDao()
    }

    @Provides
    @Singleton
    fun provideDocumentDao(database: VaiaDatabase): DocumentDao {
        return database.documentDao()
    }

    @Provides
    @Singleton
    fun provideTripRepository(
        apiService: VaiaApiService,
        tripDao: TripDao
    ): TripRepository {
        return TripRepositoryImpl(apiService, tripDao)
    }

    @Provides
    @Singleton
    fun provideActivityRepository(
        apiService: VaiaApiService,
        activityDao: ActivityDao
    ): ActivityRepository {
        return ActivityRepositoryImpl(apiService, activityDao)
    }

    @Provides
    @Singleton
    fun providePackingRepository(
        apiService: VaiaApiService,
        packingDao: PackingDao
    ): PackingRepository {
        return PackingRepositoryImpl(apiService, packingDao)
    }

    @Provides
    @Singleton
    fun provideCurrencyRepository(
        apiService: CurrencyApiService
    ): CurrencyRepository {
        return CurrencyRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: VaiaApiService,
        dataStore: DataStore<Preferences>
    ): AuthRepository {
        return AuthRepositoryImpl(apiService, dataStore)
    }

    @Provides
    @Singleton
    fun provideExpenseRepository(
        apiService: VaiaApiService
    ): ExpenseRepository {
        return ExpenseRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideDocumentRepository(
        apiService: VaiaApiService,
        documentDao: DocumentDao
    ): DocumentRepository {
        return DocumentRepositoryImpl(apiService, documentDao)
    }

    @Provides
    @Singleton
    fun provideReminderScheduler(
        @ApplicationContext context: Context
    ): ReminderScheduler {
        return ReminderScheduler(context)
    }

    @Provides
    @Singleton
    fun provideSyncManager(
        connectivityObserver: ConnectivityObserver,
        activityRepository: ActivityRepository,
        packingRepository: PackingRepository,
        activityDao: ActivityDao,
        packingDao: PackingDao
    ): SyncManager {
        return SyncManager(
            connectivityObserver,
            activityRepository,
            packingRepository,
            activityDao,
            packingDao
        )
    }
}

