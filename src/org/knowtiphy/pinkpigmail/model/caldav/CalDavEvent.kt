package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Calendar
import com.calendarfx.model.Entry
import org.apache.jena.datatypes.xsd.XSDDateTime
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.PPPeer
import java.time.LocalDate
import java.time.LocalTime

class CalDavEvent(accountId: String, storage: IStorage) : PPPeer(accountId, storage)
{
    val event = Entry<String>("")
    lateinit var calendar: Calendar

    init
    {
        declareU(Vocabulary.HAS_SUMMARY) { event.title = it.literal.string }
        declareU(Vocabulary.HAS_DATE_START, ::setStartDate)
        declareU(Vocabulary.HAS_DATE_END, ::setEndDate)
        declareU(Vocabulary.HAS_DURATION, ::setDuration)
    }

    private fun setStartDate(stmt: Statement)
    {
        val dt = stmt.literal.value as XSDDateTime
        event.changeStartDate(LocalDate.of(dt.years, dt.months, dt.days))
        event.changeStartTime(LocalTime.of(dt.hours, dt.minutes))
    }

    private fun setEndDate(stmt: Statement)
    {
        val dt = stmt.literal.value as XSDDateTime
        event.changeEndDate(LocalDate.of(dt.years, dt.months, dt.days))
        event.changeEndTime(LocalTime.of(dt.hours, dt.minutes))
    }

    private fun setDuration(stmt: Statement)
    {
        println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")
        println(stmt)
    }
}