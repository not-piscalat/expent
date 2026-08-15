package com.expent.app

import android.app.Application
import com.expent.app.data.repository.CategoryRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ExpentApplication : Application() {

    @Inject
    lateinit var categoryRepository: CategoryRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            categoryRepository.seedDefaultsIfEmpty()
        }
    }
}
