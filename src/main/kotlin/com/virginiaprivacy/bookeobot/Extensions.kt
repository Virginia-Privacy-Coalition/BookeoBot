package com.virginiaprivacy.bookeobot

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.DecimalFormat

internal fun String.internationalize(): String {
    return if (startsWith("+1")) this else if (startsWith("1")) "+${this}" else "+1${this}"
}

internal fun getSystemValue(key: String) =
    (System.getProperty(key) ?: System.getenv(key))
        ?: throw RuntimeException("Property $key is not set! Add it as a commandline option via -D$key=value or add it as an environmental variable.")

internal val dollarFormatter = DecimalFormat("###,###,##0.00")
internal val log: Logger = LoggerFactory.getLogger(Scraper::class.java)