package com.virginiaprivacy.bookeobot

import io.ktor.http.*
import io.ktor.util.date.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import java.util.*

class ScraperTest {


    private val scraper = Scraper(rssUrl = "")

    @Test
    fun getFeed() {
        runBlocking {
            val feed = scraper.feed.first()
            val startTime = Date.from(Instant.ofEpochMilli(System.currentTimeMillis()))
            val pub = feed.pubDate?.fromHttpToGmtDate() ?: GMTDate.START
            assert(pub != GMTDate.START)
            log.info(pub.toJvmDate().toGMTString())
            assert(startTime.after(pub.toJvmDate()))
            feed.eventsList?.forEach {
                if (it.publishedDate.toJvmDate() <= (startTime)) {
                    log.info("${it.publishedDate.toJvmDate().toLocaleString()} before ${startTime.toLocaleString()}")
                } else {
                    log.info("${it.publishedDate.toJvmDate().toLocaleString()} after ${startTime.toLocaleString()}")
                }
            }
        }
    }
}