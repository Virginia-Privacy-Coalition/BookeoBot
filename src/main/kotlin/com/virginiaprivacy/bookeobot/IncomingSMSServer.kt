package com.virginiaprivacy.bookeobot

import com.vonage.client.incoming.MessageEvent
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.post
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class IncomingSMSServer(private val bookeoBot: BookeoBot) {
    private val webApiScope = CoroutineScope(Executors.newFixedThreadPool(1).asCoroutineDispatcher())

    private val webApp = Javalin.create()

    suspend fun monitorForSubscriptions() {
        withContext(webApiScope.coroutineContext) {
            webApp.routes {
                post("inbound") { ctx ->
                    val event = MessageEvent.fromJson(ctx.body())
                    if (event.text.contains("subscribe", true)) {
                        bookeoBot.addNumber(event.msisdn)
                    } else if (event.text.contains("unsubscribe", true)) {
                        bookeoBot.deleteNumber(event.msisdn)
                    }
                    if (event.msisdn.lowercase().contains(bookeoBot.developerNumber.replace("+", ""))) {
                        when (event.text.escapeIfNeeded().trim().lowercase()) {
                            "bal" -> {
                                bookeoBot.sendDeveloperMessage("Cost this session: ${
                                    dollarFormatter.format(bookeoBot.persistence.usageEstimate)}")
                            }
                        }
                    }
                    else {
                        println(event.msisdn)
                    }
                    log.info("New text received from ${event.msisdn} [${event.messageTimestamp}]: ${event.text}")
                }

            }.start(8082)
                .get("status") { ctx ->
                    ctx.result("OK")
                }
        }

    }

}