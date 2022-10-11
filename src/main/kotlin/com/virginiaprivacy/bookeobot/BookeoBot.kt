package com.virginiaprivacy.bookeobot

import io.ktor.http.*
import io.ktor.util.date.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.concurrent.thread

private fun String.internationalize(): String {
    return if (startsWith("+1")) this else if (startsWith("1")) "+${this}" else "+1${this}"
}

class BookeoBot(
    private val sendShutdownText: Boolean = false,
    val developerNumber: String = "",
    private val skipMissedEvents: Boolean = false
) {

    private val startTime = Date.from(
        Instant.ofEpochMilli(System.currentTimeMillis()))

    private val scraper = Scraper()

    val persistence by lazy { Persistence.getPersistence() }

    private val inboundSmsHandler by lazy { IncomingSMSServer(this) }

    val smsClient: SMSClient = VonageClient("+18554011112" ?: "540test", this)

    fun deleteNumber(number: String) {
        val escapedNumber = number.internationalize()
        if (persistence.numbers.remove(escapedNumber)) {
            smsClient.sendMessage(escapedNumber, """You have unsubscribed to bookeo event texts. Text "SUBSCRIBE" to re-subscribe. """)
            persistence.savePhoneNumbers()
        }
    }

    fun addNumber(number: String) {
        val escapedNumber = number.internationalize()
        if (!persistence.numbers.any { it.contains(escapedNumber) }) {
            smsClient.sendMessage(escapedNumber, """You have subscribed to bookeo event texts. Text "unsubscribe" to cancel. """)
            persistence.numbers.add(escapedNumber)
            persistence.savePhoneNumbers()
        }
    }

    suspend fun startTextingEvents() {
        Runtime.getRuntime().addShutdownHook(thread(start = false, name = "ShutdownMessageHook") {
            if (sendShutdownText) {
                smsClient.sendMessage(
                    developerNumber ?: "",
                    "Bookeo bot shutting down at ${GMTDate(System.currentTimeMillis()).toHttpDate()}"
                )
            }
            persistence.save()
        })
        inboundSmsHandler.monitorForSubscriptions()
            scraper
                .feed
                .filter { it.eventsList != null }
                .collect { feed ->
                    log.info("Collected feed from ${feed.pubDate}. Events: ${feed.eventsList?.size}")
                    val events = feed.eventsList ?: emptyList()
                    for (it in events) {
                        if (skipMissedEvents) {
                            if (it.publishedDate.toJvmDate().before(startTime)) {
                                log.debug("Skipping over event: ${it.title} that was published on ${it.publishedDate.toHttpDate()}.")
                                continue
                            }
                        }
                        if (!persistence.eventProcessed(it)) {
                            smsClient.sendMessageToAllNumbers(it)
                        }
                    }
                }
        }




    class Persistence private constructor() {

        private val numbersFile = File(".numbers").apply { if (!exists()) createNewFile() }
        private val eventsFile = File(".events").apply { if (!exists()) createNewFile() }
        internal val numbers = (try { Json.decodeFromString<List<String>>(numbersFile.readText()) } catch (e: Exception) { null } ?: emptyList()).toMutableList()
        private val events = (try { Json.decodeFromString<List<Int>>(eventsFile.readText()) } catch (e: Exception) { null } ?: emptyList()).toMutableList()
        @Volatile var usageEstimate = 0.00

        fun eventProcessed(event: Event): Boolean {
            return if (events.contains(event.hashCode())) {
                true
            } else {
                events.add(event.hashCode())
                saveEvents()
                false
            }
        }

        fun save() {
            saveEvents()
            savePhoneNumbers()
        }

        @Synchronized private fun saveEvents() {
            eventsFile.writeText(Json.encodeToString(events.toList()))
        }

        fun numbers() = numbers.toList()

        @Synchronized
        fun savePhoneNumbers() {
            numbersFile.writeText(Json.encodeToString(numbers.toList()))
        }

        companion object {
            private var _persistence: Persistence? = null

            fun getPersistence(): Persistence = _persistence ?: Persistence().apply { _persistence = this }
        }

    }
}

fun main() {
    log
    val bot = BookeoBot(true, developerNumber = "", true)
    runBlocking {
        bot.startTextingEvents()
    }
}