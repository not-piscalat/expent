package com.expent.app.di

import com.expent.app.data.auth.AuthRepository
import com.expent.app.data.sync.DebtRemoteStore
import com.expent.app.data.sync.DebtSyncer
import com.expent.app.data.sync.FirestoreDebtStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindDebtRemoteStore(impl: FirestoreDebtStore): DebtRemoteStore

    companion object {

        @Provides
        @Singleton
        fun provideSyncScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** The signed-in uid, or null when signed out; drives [DebtSyncer] start/stop. */
        @Provides
        @Singleton
        fun provideUserUidFlow(authRepository: AuthRepository): Flow<String?> =
            authRepository.authState.map { it?.uid }
    }
}
