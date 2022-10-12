package com.virginiaprivacy.bookeobot

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import org.simpleframework.xml.core.Persister
import java.nio.charset.Charset
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.minutes

class Scraper(val rssUrl: String = getSystemValue("rssUrl")) {
    private val client = HttpClient(CIO)

    private val serializer = Persister()

    private val feedScope = CoroutineScope(Executors.newFixedThreadPool(1).asCoroutineDispatcher())

    private val feedFeed = flow {
        var consecutiveErrors = 0
        while (currentCoroutineContext().isActive) {
            try {
                val text = client
                    .get(rssUrl)
                    .bodyAsText(Charset.forName("UTF-16"))
                    .trim()
                    .replaceFirst("^([\\W]+)<", "<")
                emit(serializer.read(Feed::class.java, text))
                consecutiveErrors = 0
            } catch (e: Throwable) {
                consecutiveErrors++
                log.error("Failed to connect to RSS feed. Waiting several minutes then trying again. . .")
                delay(3.0.minutes)
            }
            delay(2.0.minutes)
        }
    }

    val feed = feedFeed
        .shareIn(feedScope, SharingStarted.Eagerly)

}

