package com.virginiaprivacy.bookeobot
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object Persistence {

    private val numbersFile = File(".numbers").apply { if (!exists()) createNewFile() }
    private val eventsFile = File(".events").apply { if (!exists()) createNewFile() }
    private val numbers = (try { Json.decodeFromString<List<String>>(numbersFile.readText()) } catch (e: Exception) { null } ?: emptyList()).toMutableList()
    private val events = (try { Json.decodeFromString<List<Int>>(eventsFile.readText()) } catch (e: Exception) { null } ?: emptyList()).toMutableList()
    @Volatile var usageEstimate = 0.00

    fun eventProcessed(event: Event): Boolean {
        return if (events.contains(event.hashCode())) {
            true
        } else {
            events.add(event.hashCode())
            saveEvents()
            false
        }
    }

    fun save() {
        saveEvents()
        savePhoneNumbers()
    }

    @Synchronized private fun saveEvents() {
        eventsFile.writeText(Json.encodeToString(events.toList()))
    }

    fun numbers() = numbers.toList()

    fun deleteNumber(number: String) {
        val escapedNumber = number.internationalize()
        if (numbers.remove(escapedNumber)) {
            println(
                getMessage(escapedNumber,
                """You have unsubscribed to bookeo event texts. Text "SUBSCRIBE" to re-subscribe. """)
                .submit().messages[0])
            savePhoneNumbers()
        }
    }

    fun addNumber(number: String) {
        val escapedNumber = number.internationalize()
        if (!numbers.any { it.contains(escapedNumber) }) {
            println(
                getMessage(escapedNumber, """You have subscribed to bookeo event texts. Text "CANCEL" to cancel. """)
                .submit().messages[0])
            numbers.add(escapedNumber)
            savePhoneNumbers()
        }
    }

    private fun String.internationalize(): String {
        return if (startsWith("+1")) this else if (startsWith("1")) "+${this}" else "+1${this}"
    }

    @Synchronized private fun savePhoneNumbers() {
        numbersFile.writeText(Json.encodeToString(numbers.toList()))
    }

}