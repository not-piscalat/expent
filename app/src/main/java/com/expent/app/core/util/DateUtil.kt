package com.expent.app.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtil {

    private val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

    fun format(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
}
