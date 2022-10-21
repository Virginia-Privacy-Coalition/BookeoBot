package com.virginiaprivacy.bookeobot

class ConsoleOutputSMSClient(
    defaultOriginNumber: String = "8675309"
) : SMSClient(defaultOriginNumber) {

    override fun sendMessage(destinationNumber: String, persistence: Persistence, body: String) {
        log.info("Sending SMS to $destinationNumber: $body")
    }

    override fun sendMessage(destinationNumber: String, persistence: Persistence, originNumber: String, body: String) {
        log.info("Sending SMS from $originNumber to $destinationNumber: $body")
    }

    override fun sendMessageToAllNumbers(event: Event, persistence: Persistence) {
        log.info("Sending message to all subscribed numbers for event $event")
    }

    override fun sendMessageToAllNumbers(event: Event, persistence: Persistence, originNumber: String) {
        log.info("Sending message from $originNumber to all subscribed numbers for event $event")
    }

    override fun sendMessageToAllNumbers(persistence: Persistence, body: String) {
        log.info("sending message to all numbers: $body")
    }
}