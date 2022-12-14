package com.virginiaprivacy.bookeobot

import com.vonage.client.VonageClient
import com.vonage.client.sms.MessageStatus
import com.vonage.client.sms.SmsSubmissionResponse
import com.vonage.client.sms.messages.TextMessage

class VonageClient(defaultOriginNumber: String,
                   private val apiKey: String = getSystemValue("VONAGE_API_KEY"),
                   private val apiSecret: String = getSystemValue("VONAGE_API_SECRET")
) : SMSClient(defaultOriginNumber) {

    private val client: VonageClient = VonageClient.builder().apiKey(apiKey).apiSecret(apiSecret).build()
    private val smsClient = client.smsClient

    private fun getMessage(destination: String, body: String, originNumber: String): TextMessage =
        TextMessage(originNumber, destination, body)

    private fun TextMessage.submit(persistence: Persistence): SmsSubmissionResponse {
        return smsClient.submitMessage(this)
            .also {
                for (messageIndex in 0.until(it.messageCount)) {
                    println(it.messages[messageIndex])
                    persistence
                        .usageEstimate
                        .getAndUpdate { est ->
                            est + (it.messages[messageIndex].messagePrice * 100.0.toBigDecimal()).toInt()
                        }                }
                log.info("Total cost estimate: ${dollarFormatter.format(persistence.usageEstimate.get() / 100)}")
            }
    }

    override fun sendMessage(destinationNumber: String, persistence: Persistence, body: String) {
        getMessage(destinationNumber, body, defaultOriginNumber).submit(persistence)
    }

    override fun sendMessage(destinationNumber: String, persistence: Persistence, originNumber: String, body: String) {
        getMessage(destinationNumber, body, originNumber).submit(persistence)
    }

    override fun sendMessageToAllNumbers(event: Event, persistence: Persistence) {
        sendEventMessage(event, persistence = persistence)
    }

    private fun sendEventMessage(event: Event, persistence: Persistence, originNumber: String = defaultOriginNumber) {
        persistence.numbers().forEach { number ->
            getMessage(number, event.title + "\n" + event.textDescription, originNumber)
                .submit(persistence)

        }
    }

    override fun sendMessageToAllNumbers(event: Event, persistence: Persistence, originNumber: String) {
        sendEventMessage(event, persistence, originNumber)
    }

    override fun sendMessageToAllNumbers(persistence: Persistence, body: String) {
        persistence.numbers().forEach { number ->
            getMessage(number, body, defaultOriginNumber)
                .submit(persistence)
                .run {
                    if (messageCount > 0) {
                        messages
                            .filterNotNull()
                            .forEach {
                                if (it.status != MessageStatus.OK) {
                                    log.warn("Problem sending SMS message [${it.status.name}]: $it")
                                }
                                persistence
                                    .usageEstimate
                                    .getAndUpdate { est ->
                                    est + (it.messagePrice * 100.0.toBigDecimal()).toInt()
                                }
                            }
                    }
                }
        }
    }
}