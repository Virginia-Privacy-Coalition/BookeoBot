package com.virginiaprivacy.bookeobot

import io.javalin.http.Context


abstract class SMSClient(val defaultOriginNumber: String, val bookeoBot: BookeoBot) {

    abstract fun sendMessage(destinationNumber: String, body: String)
    abstract fun sendMessage(destinationNumber: String, originNumber: String, body: String)
    abstract fun sendMessageToAllNumbers(event: Event)
    abstract fun sendMessageToAllNumbers(event: Event, originNumber: String = defaultOriginNumber)

}

