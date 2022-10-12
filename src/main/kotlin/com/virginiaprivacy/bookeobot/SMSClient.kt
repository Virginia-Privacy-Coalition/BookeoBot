package com.virginiaprivacy.bookeobot


abstract class SMSClient(val defaultOriginNumber: String) {

    abstract fun sendMessage(destinationNumber: String, persistence: BookeoBot.Persistence, body: String)
    abstract fun sendMessage(destinationNumber: String, persistence: BookeoBot.Persistence, originNumber: String, body: String)
    abstract fun sendMessageToAllNumbers(event: Event, persistence: BookeoBot.Persistence)
    abstract fun sendMessageToAllNumbers(event: Event, persistence: BookeoBot.Persistence, originNumber: String = defaultOriginNumber)
    abstract fun sendMessageToAllNumbers(persistence: BookeoBot.Persistence, body: String)

}

