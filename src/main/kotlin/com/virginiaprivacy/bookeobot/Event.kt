package com.virginiaprivacy.bookeobot

import io.ktor.http.*
import io.ktor.util.date.*
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "item", strict = false)
data class Event @JvmOverloads constructor(
    @field:Element(name = "title")
    @param:Element(name = "title")
    val title: String? = null,

    @field:Element(name = "link")
    @param:Element(name = "link")
    var link: String? = null,

    @field:Element(name = "description")
    @param:Element(name = "description")
    private val description: String? = null,

    @field:Element(name = "pubDate")
    @param:Element(name = "pubDate")
    private val pubDate: String? = null
) {
    val date = pubDate?.fromHttpToGmtDate() ?: GMTDate.START

    val textDescription = description?.replace("<br/>", "\n")?.replace(Regex("\\<[^>]*>"), "") ?: ""


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Event

        if (title != other.title) return false
        if (pubDate != other.pubDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (pubDate?.hashCode() ?: 0)
        return result
    }
}
