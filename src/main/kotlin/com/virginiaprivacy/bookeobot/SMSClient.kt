package com.virginiaprivacy.bookeobot


abstract class SMSClient(val defaultOriginNumber: String) {

    abstract fun sendMessage(destinationNumber: String, persistence: Persistence, body: String)
    abstract fun sendMessage(destinationNumber: String, persistence: Persistence, originNumber: String, body: String)
    abstract fun sendMessageToAllNumbers(event: Event, persistence: Persistence)
    abstract fun sendMessageToAllNumbers(event: Event, persistence: Persistence, originNumber: String = defaultOriginNumber)
    abstract fun sendMessageToAllNumbers(persistence: Persistence, body: String)

}

