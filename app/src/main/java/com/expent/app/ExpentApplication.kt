package com.expent.app

import android.app.Application
import com.expent.app.data.recurring.RecurringEngine
import com.expent.app.data.repository.CategoryRepository
import com.expent.app.data.sync.DebtSyncer
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
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

    @Inject
    lateinit var recurringEngine: RecurringEngine

    @Inject
    lateinit var debtSyncer: DebtSyncer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Crash reporting activates automatically once google-services.json is present.
        // Without a Firebase config, initializeApp returns null and this is a no-op.
        if (FirebaseApp.initializeApp(this) != null) {
            FirebaseCrashlytics.getInstance()
        }
        // Starts the mutual-debt sync engine; it idles until the user signs in.
        debtSyncer.start()
        applicationScope.launch {
            categoryRepository.seedDefaultsIfEmpty()
            recurringEngine.applyDue()
        }
    }
}
