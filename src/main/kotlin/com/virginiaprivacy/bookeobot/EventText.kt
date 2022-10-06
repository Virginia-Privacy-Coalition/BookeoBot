package com.virginiaprivacy.bookeobot

import io.ktor.util.date.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import java.util.Queue
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class EventText(
    val destination: String,
    val messageBody: String,
    val queueTime: Long
) {
    @Transient val validForSend: Boolean
        get() =
            getTimeMillis().minus(queueTime).toDuration(DurationUnit.HOURS).inWholeHours > 8


}


