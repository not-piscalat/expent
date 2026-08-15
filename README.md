# Expent 💸

An Android expense tracker for **money logs** (expenses & income), **categories**, and **debt tracking** (money lent or borrowed).

This is the groundwork: a complete, buildable project skeleton with the data layer, dependency injection, and navigation shell in place — ready to start building features on top.

## Tech stack

| Layer        | Choice                                                        |
| ------------ | ------------------------------------------------------------- |
| Language     | Kotlin 2.1                                                     |
| UI           | Jetpack Compose (Material 3)                                  |
| Architecture | MVVM — single-activity, Compose + ViewModel + StateFlow       |
| Persistence  | Room 2.6 (coroutines + Flow support)                          |
| DI           | Hilt 2.53                                                     |
| Navigation   | Navigation Compose (bottom-nav shell)                         |
| Build        | Gradle 8.11.1 (wrapper), AGP 8.7.3, Kotlin DSL + version catalog |
| Min / Target | minSdk 26, targetSdk 35 (compileSdk 35)                       |

## Project structure

```
app/src/main/java/com/expent/app/
├── ExpentApplication.kt          # Hilt app; seeds default categories on first launch
├── MainActivity.kt               # Single activity hosting the Compose UI
├── core/util/                    # MoneyUtil (minor-unit formatting), DateUtil
├── data/
│   ├── local/
│   │   ├── ExpentDatabase.kt     # Room database (v1)
│   │   ├── dao/                  # TransactionDao, CategoryDao, DebtDao, DebtPaymentDao
│   │   └── entity/               # Transaction, Category, Debt, DebtPayment + type enums
│   ├── repository/               # Thin repository classes over the DAOs
│   └── seed/DefaultCategories.kt # Out-of-the-box expense/income categories
├── di/DatabaseModule.kt          # Hilt: provides database + DAOs
└── ui/
    ├── ExpentApp.kt              # Bottom-nav shell (Home / Transactions / Debts)
    ├── theme/                    # Material 3 emerald theme (light + dark)
    ├── components/               # CategoryIcons, TransactionRow, EmptyState
    ├── home/                     # Monthly balance + recent activity
    ├── transactions/             # All transactions list
    └── debts/                    # Debts list with remaining balances
```

## Data model

- **transactions** — a unified money log: expense or income (`type`), `amountCents`, optional `categoryId` (FK, SET_NULL on delete), `note`, `timestamp`.
- **categories** — typed per transaction type (EXPENSE / INCOME), with a stable `iconName` key and ARGB `colorArgb`. Seeded with sensible defaults on first launch.
- **debts** — money you lent or borrowed (`type` = LENT / BORROWED), `amountCents`, optional person + due date. The remaining balance is derived from payments, never stored.
- **debt_payments** — partial payments against a debt (FK, CASCADE on delete).

All money is stored as **integer minor units (cents)** — never floating point.

## Getting started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (any recent version; the bundled JDK works fine)
- Android SDK Platform 35 (Android Studio will offer to install it)

### Run it

1. Open the project folder in Android Studio and let it sync (it downloads Gradle + dependencies on first sync).
2. Run the `app` configuration on an emulator or device.

### Build from the command line

```bash
# Point Gradle at your SDK (or set ANDROID_HOME):
echo "sdk.dir=C\:/Users/<you>/AppData/Local/Android/Sdk" > local.properties

./gradlew assembleDebug
./gradlew test          # runs the JVM unit tests
```

`local.properties` is machine-specific and git-ignored.

## Roadmap (next steps)

- [ ] Add-transaction flow (amount, type, category picker, date, note)
- [ ] Add-debt flow + record payments
- [ ] Category management screen (add/edit/delete, custom icons & colors)
- [ ] Filtering, search, and month-by-month browsing
- [ ] Currency setting (symbol currently not shown; `MoneyUtil` is the single place to add it)
- [ ] Export (CSV) and backups
