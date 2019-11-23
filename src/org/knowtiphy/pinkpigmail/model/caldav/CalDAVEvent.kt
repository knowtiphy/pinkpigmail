package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Calendar
import com.calendarfx.model.Entry
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.PPPeer
import java.time.*
import java.time.format.DateTimeFormatter

class CalDAVEvent(accountId: String, storage: IStorage) : PPPeer(accountId, storage)
{
    val event = Entry<String>("")
    lateinit var calendar: Calendar

    init
    {
        declareU(Vocabulary.HAS_SUMMARY) { event.title = it.literal.string }
        declareU(Vocabulary.HAS_DATE_START, ::setStartDate)
        declareU(Vocabulary.HAS_DATE_END, ::setEndDate)
    }

    private fun setStartDate(stmt: Statement)
    {
        val dt = ZonedDateTime.parse(stmt.literal.string, DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        event.changeStartDate(dt.toLocalDate())
        event.changeStartTime(dt.toLocalTime())
    }

    private fun setEndDate(stmt: Statement)
    {
        val dt = ZonedDateTime.parse(stmt.literal.string, DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        event.changeEndDate(dt.toLocalDate())
        event.changeEndTime(dt.toLocalTime())
    }
}