package com.expent.app.core

import com.expent.app.core.util.MoneyUtil

/** Pure save-enablement rules shared by the app's forms. */
object FormValidation {

    fun isValidAmount(input: String): Boolean =
        MoneyUtil.parse(input)?.let { it > 0 } == true

    fun canSaveTransaction(amountInput: String): Boolean =
        isValidAmount(amountInput)

    fun canSaveDebt(title: String, amountInput: String): Boolean =
        title.isNotBlank() && isValidAmount(amountInput)

    fun canSaveCategory(name: String): Boolean =
        name.isNotBlank()
}
