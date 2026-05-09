package com.vaia.di

import android.content.Context
import com.vaia.data.api.VaiaApiService
import com.vaia.data.local.db.ActivityDao
import com.vaia.data.local.db.PackingDao
import com.vaia.data.local.db.TripDao
import com.vaia.data.local.db.VaiaDatabase
import com.vaia.data.network.ConnectivityObserver
import com.vaia.data.repository.ActivityRepositoryImpl
import com.vaia.data.repository.PackingRepositoryImpl
import com.vaia.data.repository.TripRepositoryImpl
import com.vaia.data.sync.SyncManager
import com.vaia.domain.repository.ActivityRepository
import com.vaia.domain.repository.PackingRepository
import com.vaia.domain.repository.TripRepository
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
        apiService: VaiaApiService
    ): PackingRepository {
        return PackingRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideSyncManager(
        connectivityObserver: ConnectivityObserver,
        activityRepository: ActivityRepository,
        packingRepository: PackingRepository,
        tripRepository: TripRepository,
        activityDao: ActivityDao,
        packingDao: PackingDao,
        tripDao: TripDao
    ): SyncManager {
        return SyncManager(
            connectivityObserver,
            activityRepository,
            packingRepository,
            tripRepository,
            activityDao,
            packingDao,
            tripDao
        )
    }
}
