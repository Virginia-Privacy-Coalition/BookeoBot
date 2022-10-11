package com.virginiaprivacy.bookeobot

import io.javalin.http.Context

class ConsoleOutputSMSClient(defaultOriginNumber: String = "8675309",
                             bookeoBot: BookeoBot
) : SMSClient(defaultOriginNumber, bookeoBot) {

    override fun sendMessage(destinationNumber: String, body: String) {
        log.info("Sending SMS to $destinationNumber: $body")
    }

    override fun sendMessage(destinationNumber: String, originNumber: String, body: String) {
        log.info("Sending SMS from $originNumber to $destinationNumber: $body")
    }

    override fun sendMessageToAllNumbers(event: Event) {
        log.info("Sending message to all subscribed numbers for event $event")
    }

    override fun sendMessageToAllNumbers(event: Event, originNumber: String) {
        log.info("Sending message from $originNumber to all subscribed numbers for event $event")
    }
}