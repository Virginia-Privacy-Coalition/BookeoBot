package com.virginiaprivacy.bookeobot

import io.ktor.util.cio.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class Persistence private constructor() {

    private val numbersFile = File(".numbers").apply { if (!exists()) createNewFile() }
    internal val numbers = CopyOnWriteArrayList((try { Json.decodeFromString<List<String>>(numbersFile.readText()) } catch (e: Exception) { null } ?: emptyList()).toMutableList())
    val usageEstimate: AtomicInteger = AtomicInteger(0)

    fun save() {
        savePhoneNumbers()
    }

    fun numbers() = numbers

    @Synchronized
    fun savePhoneNumbers() {
        numbersFile.writeText(Json.encodeToString(numbers.toList()))
    }

    companion object {
        private var _persistence: Persistence? = null

        fun getPersistence(): Persistence = _persistence ?: Persistence().apply { _persistence = this }
    }

}