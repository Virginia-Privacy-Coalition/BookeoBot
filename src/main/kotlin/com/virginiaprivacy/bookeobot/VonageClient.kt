package com.virginiaprivacy.bookeobot

import com.vonage.client.VonageClient
import com.vonage.client.incoming.MessageEvent
import com.vonage.client.sms.MessageStatus
import com.vonage.client.sms.SmsSubmissionResponse
import com.vonage.client.sms.messages.TextMessage
import io.javalin.http.Context

class VonageClient(defaultOriginNumber: String, bookeoBot: BookeoBot) : SMSClient(defaultOriginNumber, bookeoBot) {
    private val apiKey = getSystemValue("VONAGE_API_KEY")
    private val apiSecret = getSystemValue("VONAGE_API_SECRET")

    private val client: VonageClient = VonageClient.builder().apiKey(apiKey).apiSecret(apiSecret).build()
    private val smsClient = client.smsClient
    private val persistence = bookeoBot.persistence

    private fun getMessage(destination: String, body: String, originNumber: String): TextMessage =
        TextMessage(originNumber, destination, body)

    private fun TextMessage.submit(): SmsSubmissionResponse {
        return smsClient.submitMessage(this)
            .also {
                for (messageIndex in 0.until(it.messageCount)) {
                    println(it.messages[messageIndex])
                    persistence.usageEstimate += (it.messages[messageIndex].messagePrice).toDouble()
                }
                log.info("Total cost estimate: ${dollarFormatter.format(persistence.usageEstimate)}")
            }
    }

    override fun sendMessage(destinationNumber: String, body: String) {
        getMessage(destinationNumber, body, defaultOriginNumber).submit()
    }

    override fun sendMessage(destinationNumber: String, originNumber: String, body: String) {
        getMessage(destinationNumber, body, originNumber).submit()
    }

    override fun sendMessageToAllNumbers(event: Event) {
        sendEventMessage(event)
    }

    private fun sendEventMessage(event: Event, originNumber: String = defaultOriginNumber) {
        persistence.numbers().forEach { number ->
            getMessage(number, event.title + "\n" + event.textDescription, originNumber)
                .submit()
                .run {
                    if (messageCount > 0) {
                        messages
                            .filterNotNull()
                            .forEach {
                                if (it.status != MessageStatus.OK) {
                                    log.warn("Problem sending SMS message [${it.status.name}]: $it")
                                }
                                persistence.usageEstimate += it.messagePrice.toDouble()
                            }
                    }
                }
        }
    }

    override fun sendMessageToAllNumbers(event: Event, originNumber: String) {
        sendEventMessage(event, originNumber)
    }
}