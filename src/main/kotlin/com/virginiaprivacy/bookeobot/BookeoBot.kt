package com.virginiaprivacy.bookeobot

import io.ktor.http.*
import io.ktor.util.date.*
import kotlinx.coroutines.flow.*
import kotlin.concurrent.thread

class BookeoBot(val smsClient: SMSClient, val sendShutdownText: Boolean = false, val developerNumber: String? = null) {
    private val scraper = Scraper()

    suspend fun startTextingEvents() {
        val shutdownHook = thread(start = false, name = "ShutdownMessageHook") {
            if (sendShutdownText) {
                smsClient.sendMessage(
                    developerNumber ?: "",
                    "Bookeo bot shutting down at ${GMTDate(System.currentTimeMillis()).toHttpDate()}"
                )
            }
            Persistence.save()
        }
        scraper
            .getFeed()
            .onSubscription {
                Runtime.getRuntime().addShutdownHook(shutdownHook)
            }
            .filter { it.eventsList != null }
            .collect { feed ->
                feed.eventsList?.filter { !Persistence.eventProcessed(it) }?.forEach {
                    smsClient.sendMessageToAllNumbers(it)
                }
            }
    }


}