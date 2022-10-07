package com.virginiaprivacy.bookeobot

import com.vonage.client.VonageClient
import com.vonage.client.incoming.MessageEvent
import com.vonage.client.sms.SmsSubmissionResponse
import com.vonage.client.sms.messages.TextMessage
import io.javalin.Javalin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.date.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.simpleframework.xml.core.Persister
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.text.DecimalFormat
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.minutes

class Scraper {
    private val client = HttpClient(CIO)

    private val serializer = Persister()

    private val feedScope = CoroutineScope(Executors.newFixedThreadPool(1).asCoroutineDispatcher())


    suspend fun getFeed() = flow {
        var consecutiveErrors = 0
        while (currentCoroutineContext().isActive) {
            try {
                val text = client
                    .get(rssUrl)
                    .bodyAsText(Charset.forName("UTF-16"))
                    .trim()
                    .replaceFirst("^([\\W]+)<", "<")
                consecutiveErrors = 0
                emit(serializer.read(Feed::class.java, text))
            } catch (e: Throwable) {
                consecutiveErrors++
                log.error("Failed to connect to RSS feed. Waiting several minutes then trying again. . .")
                delay(3.0.minutes)
            }
            delay(2.0.minutes)
        }
    }
        .shareIn(feedScope, SharingStarted.Eagerly)

    companion object {
        val rssUrl = getSystemValue("rssUrl")

    }
}

fun getSystemValue(key: String) =
    (System.getProperty(key) ?: System.getenv(key))
        ?: throw RuntimeException("Property $key is not set! Add it as a commandline option via -D$key=value or add it as an environmental variable.")


fun main() {
    val scraper = Scraper()
    val webApiScope = CoroutineScope(Executors.newFixedThreadPool(1).asCoroutineDispatcher())

    runBlocking {
        webApiScope.launch {
            val app = Javalin.create().start(8082)
            app.post("inbound") { ctx ->
                val event = MessageEvent.fromJson(ctx.body())
                if (event.text.contains("subscribe", true)) {
                    Persistence.addNumber(event.msisdn)
                } else if (event.text.contains("unsubscribe", true)) {
                    Persistence.deleteNumber(event.msisdn)
                }
                log.info("New text received from ${event.msisdn} [${event.messageTimestamp}]: ${event.text}")
            }

        }
    }
}

val dollarFormatter = DecimalFormat("###,###,##0.00")
val log: Logger = LoggerFactory.getLogger(Scraper::class.java)