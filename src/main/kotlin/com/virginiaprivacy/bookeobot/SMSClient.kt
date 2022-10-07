package com.virginiaprivacy.bookeobot


abstract class SMSClient(val defaultOriginNumber: String) {

    abstract fun sendMessage(destinationNumber: String, body: String)
    abstract fun sendMessage(destinationNumber: String, originNumber: String, body: String)
    abstract fun sendMessageToAllNumbers(event: Event)
    abstract fun sendMessageToAllNumbers(event: Event, originNumber: String = defaultOriginNumber)

}

