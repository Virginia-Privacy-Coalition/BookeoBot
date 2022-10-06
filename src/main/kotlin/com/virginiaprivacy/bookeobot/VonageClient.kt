package com.virginiaprivacy.bookeobot

import com.vonage.client.VonageClient
import com.vonage.client.sms.MessageStatus
import com.vonage.client.sms.SmsSubmissionResponse
import com.vonage.client.sms.messages.TextMessage

class VonageClient(defaultOriginNumber: String) : SMSClient(defaultOriginNumber) {
    private val apiKey = getSystemValue("VONAGE_API_KEY")
    private val apiSecret = getSystemValue("VONAGE_API_SECRET")

    private val client: VonageClient = VonageClient.builder().apiKey(apiKey).apiSecret(apiSecret).build()
    private val smsClient = client.smsClient

    private fun getMessage(destination: String, body: String, originNumber: String): TextMessage =
        TextMessage(originNumber, destination, body)

    private fun TextMessage.submit(): SmsSubmissionResponse {
        return smsClient.submitMessage(this)
            .also {
                for (messageIndex in 0.until(it.messageCount)) {
                    Persistence.usageEstimate += (it.messages[messageIndex].messagePrice).toDouble()
                }
                log.info("Total cost estimate: ${dollarFormatter.format(Persistence.usageEstimate)}")
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
        Persistence.numbers().forEach { number ->
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
                                Persistence.usageEstimate += it.messagePrice.toDouble()
                            }
                    }
                }
        }
    }

    override fun sendMessageToAllNumbers(event: Event, originNumber: String) {
        sendEventMessage(event, originNumber)
    }
}