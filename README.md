# Expent 💸

A private, local-first expense tracker for Android. Log expenses and income, set monthly budgets, track money you lent or borrowed, and let the app forecast and flag what matters.

**No ads. No tracking. Local-first** — your data stays on your phone and you can export a JSON backup whenever you want to move it. Optionally sign in with Google to share debts and keep them in sync between phones.

## 📲 Install

Grab the latest APK from the **[releases page](https://github.com/not-piscalat/expent/releases)**, open it on your phone, and allow installs from that source. Requires Android 8.0+.

Prefer to build it yourself? See [Getting started](#getting-started).

## 📝 Changelog

### v0.3.1 — 2026-08-16

**Per-account data on shared devices** — each Google sign-in on the same phone now keeps its own view:

- Debts, transactions, categories (and their budgets), and recurring templates are visible only to the account that created them
- Currency, theme, and starting balance are per-account, with the device default as a graceful fallback
- Switching accounts never shows or materializes the other person's data

**Sync reliability**

- Deletes are now tombstone-based soft deletes that propagate reliably instead of racing the push
- Fixed a race where a stale remote snapshot could resurrect a deleted debt
- The push loop only re-writes rows that actually changed, cutting Firestore write volume

**Also fixed**: undo-snackbar deletes now always fire; sync restarts can no longer resurrect deleted debts.

### v0.3.0 — 2026-08-16

- **Mutual-debt sync** — optional Google sign-in, share codes, and two-way sync of shared debts and their payments between accounts (Firebase Auth + Firestore)

### v0.2.0 — 2026-08-15

- Crash reporting (Firebase Crashlytics), settings included in JSON backups, release signing, and the first installable signed APK

## ✨ Features

- **Transactions** — expense/income logging with categories, notes, and dates; edit by tapping, delete with undo
- **Spending by category** — monthly breakdown with per-category **budgets**, progress bars, and **on-pace warnings** ("on track to exceed by ₱1,200")
- **Debts** — track money lent or borrowed, record partial payments, see remaining balances and settlement progress
- **Recurring transactions** — monthly and weekly bills & salary logged automatically, with pause/resume
- **Smart stuff** — next-month forecast with accuracy scoring, anomaly insights (unusually large expenses, likely duplicates, missed recurring income), auto-category suggestions learned from your own history
- **Net worth** — starting cash + monthly balance + outstanding debts on one card
- **Month navigation** — browse any past month; search and filter the transaction list
- **Settings** — currency symbol (₱ / $ / none), dark & light theme, starting balance
- **Backup & restore** — export the whole database (categories, transactions, debts, payments, recurring templates, settings) to JSON via the share sheet

## 🔒 Privacy

Expent is **local-first**: everything lives in a local Room database on your device and works fully offline. There are no ads and no analytics. Two optional Firebase integrations exist: Crashlytics (crash reports only, active when `google-services.json` is added to the build) and shared-debt sync (active only after you sign in with Google).

## 🛠 Tech stack

| Layer        | Choice                                                        |
| ------------ | ------------------------------------------------------------- |
| Language     | Kotlin 2.1                                                     |
| UI           | Jetpack Compose (Material 3)                                  |
| Architecture | MVVM — single-activity, Compose + ViewModel + StateFlow       |
| Persistence  | Room 2.6 (coroutines + Flow), DataStore (settings)            |
| DI           | Hilt 2.53                                                     |
| Serialization| kotlinx-serialization (JSON backup format)                    |
| Navigation   | Navigation Compose (bottom-nav shell)                         |
| Build        | Gradle (wrapper), AGP 8.7.3, Kotlin DSL + version catalog     |
| Min / Target | minSdk 26, targetSdk 35 (compileSdk 35)                       |
| Crash reports| Firebase Crashlytics (optional; activates when `google-services.json` is added) |

## Project structure

```
app/src/main/java/com/expent/app/
├── ExpentApplication.kt      # Hilt app; seeds categories, runs the recurring engine
├── MainActivity.kt           # Single activity hosting the Compose UI
├── core/                     # Pure, unit-tested logic (no Android deps)
│   ├── util/                 # MoneyUtil (minor units), DateUtil
│   ├── TransactionFilters    # search + month filtering
│   ├── CategorySpending      # monthly breakdown aggregation
│   ├── BudgetPacing          # running-rate budget projections
│   ├── Forecast              # next-month forecast + accuracy
│   ├── Insights              # anomaly/duplicate/missed-recurring flags
│   ├── CategorySuggester     # note -> category suggestions (history-learned)
│   ├── DebtPosition          # net lent/borrowed positions
│   ├── RecurringSchedule     # monthly/weekly occurrence math
│   └── FormValidation        # save-enablement rules
├── data/
│   ├── local/                # Room database, migrations v1->v2->v3, DAOs, entities
│   ├── backup/               # JSON backup codec + transactional restore
│   ├── recurring/            # engine that materializes due occurrences
│   ├── repository/           # thin repositories over DAOs + settings
│   └── seed/DefaultCategories.kt
├── di/                       # Hilt modules
└── ui/                       # Compose screens (home, transactions, debts, categories,
                              #   recurring, settings) + components + theme
```

All money is stored as **integer minor units (cents)** — never floating point. Migration from v1 to the current schema is covered by an automated test.

## Getting started

### Prerequisites

- [Android Studio](https://developer.android.com/studio)
- Android SDK Platform 35 (Android Studio will offer to install it)

### Run it

1. Open the project folder in Android Studio and let it sync.
2. Run the `app` configuration on an emulator or device.

### Build from the command line

```bash
# Point Gradle at your SDK (or set ANDROID_HOME):
echo "sdk.dir=C\:/Users/<you>/AppData/Local/Android/Sdk" > local.properties

./gradlew assembleDebug    # debug APK
./gradlew test             # runs the JVM unit tests (92+ tests)
```

`local.properties` is machine-specific and git-ignored.

## 🔐 Release signing

Release builds are signed from a git-ignored `keystore.properties` + `app/keystore/`. If you clone the repo, run a release build without those files and it falls back to unsigned — or set up your own keystore:

```bash
keytool -genkeypair -keystore app/keystore/expent-release.jks -alias expent \
  -keyalg RSA -keysize 2048 -validity 10000
```

…then create `keystore.properties` with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.

## Roadmap

- [x] Mutual-debt sync between accounts (Firebase auth + shared debt records)
- [ ] Tagalog localization
- [ ] Home-screen widget
