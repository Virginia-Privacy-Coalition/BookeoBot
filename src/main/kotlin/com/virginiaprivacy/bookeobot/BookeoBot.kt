package com.virginiaprivacy.bookeobot

import io.ktor.http.*
import io.ktor.util.date.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.*
import kotlin.concurrent.thread


class BookeoBot(
    val developerNumber: String = "",
    private val smsClient: SMSClient,
    private val rssUrl: String,
    private val sendShutdownText: Boolean = false,
    private val skipMissedEvents: Boolean = false
) {

    @Volatile private var latestEventPublishTime = Date.from(Instant.ofEpochMilli(System.currentTimeMillis()))

    private val scraper = Scraper(rssUrl)

    val persistence by lazy { Persistence.getPersistence() }

    private val inboundSmsHandler by lazy { IncomingSMSServer(this) }

    fun deleteNumber(number: String) {
        val escapedNumber = number.internationalize()
        if (persistence.numbers.remove(escapedNumber)) {
            smsClient.sendMessage(escapedNumber, persistence,"""You have unsubscribed to bookeo event texts. Text "SUBSCRIBE" to re-subscribe. """)
            persistence.savePhoneNumbers()
        }
    }

    fun sendDeveloperMessage(messageText: String) {
        smsClient.sendMessage(developerNumber, persistence,messageText)
    }

    fun addNumber(number: String) {
        val escapedNumber = number.internationalize()
        if (!persistence.numbers.any { it.contains(escapedNumber) }) {
            smsClient.sendMessage(escapedNumber, persistence,"""You have subscribed to bookeo event texts. Text "unsubscribe" to cancel. """)
            persistence.numbers.add(escapedNumber)
            persistence.savePhoneNumbers()
        }
    }

    suspend fun startTextingEvents() {
        Runtime.getRuntime().addShutdownHook(thread(start = false, name = "ShutdownMessageHook") {
            if (sendShutdownText) {
                smsClient.sendMessageToAllNumbers(
                    persistence,
                    "Bookeobot shutting down at ${GMTDate(System.currentTimeMillis()).toHttpDate()}. " +
                            "You should check Bookeo manually until you get another text from this number."
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
                            if (it.publishedDate.toJvmDate().before(latestEventPublishTime)) {
                                log.debug("Skipping over event: ${it.title} that was published on ${it.publishedDate.toHttpDate()}.")
                            } else {
                                try {
                                    smsClient.sendMessageToAllNumbers(it, persistence)
                                } catch (e: Throwable) {
                                    log.error("Error sending messages to all numbers.")
                                    e.printStackTrace()
                                } finally {
                                    latestEventPublishTime = it.publishedDate.toJvmDate()
                                }
                            }
                        }
                    }
                }
        }


}

fun main(args: Array<String>) {

    val parser = ArgParser("bookeobot")

    val rssUrl by parser.option(
        ArgType.String,
        fullName = "rss",
        description = "The Bookeo accounts RSS feed url to scrape"
    ).required()

    val smsClientType by parser.option(
        ArgType.Choice(listOf("vonage", "console"), { it }),
        shortName = "sms",
        description = "The SMS client to use to send SMS messages"
    ).required()

    val sendShutdownText by parser.option(
        ArgType.Boolean,
        shortName = "shutdown",
        description = "Send a text message to subscribers to let them know when the bot is shutdown"
    ).default(false)

    val skipMissedEvents by parser.option(
        ArgType.Boolean,
        shortName = "skip",
        description = "Skip events that are published before the startup time of the bot"
    ).default(true)

    val apiKey by parser.option(
            ArgType.String,
            shortName = "api",
            description = "The API key for vonage. You can also set the VONAGE_API_KEY environmental variable instead."
        ).required()

    val secret by
        parser.option(
            ArgType.String,
            shortName = "secret",
            description = "The secret key for vonage. You can also set the VONAGE_API_SECRET environmental variable instead."
        ).required()

    val originNumber by parser.option(
        ArgType.String,
        shortName = "from",
        description = "The number that outgoing texts will originate from"
    ).required()

    val developerNumber by parser.option(
        ArgType.String,
        shortName = "devnumber",
        description = "The phone number of the developer to use for alerts and commands"
    ).default("8675309")

    parser.parse(args)

    val smsClient: SMSClient = when (smsClientType) {
        "vonage" -> {
            VonageClient(originNumber, apiKey, secret)
        }
        "console" -> {
            ConsoleOutputSMSClient(originNumber)
        }
        else -> {
            throw IllegalArgumentException("""The value entered "$smsClientType is not a valid sms client. """)
        }
    }

    val bot = BookeoBot(developerNumber, smsClient, rssUrl, sendShutdownText, skipMissedEvents)

    runBlocking {
        bot.startTextingEvents()
    }
}