package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Calendar
import com.calendarfx.model.Entry
import org.apache.jena.datatypes.xsd.XSDDateTime
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.PPPeer
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

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
        val dt = stmt.literal.value as XSDDateTime
        System.out.println("START " + dt.toString())
        event.changeStartDate(LocalDate.of(dt.years, dt.months, dt.days))
        event.changeStartTime(LocalTime.of(dt.hours, dt.minutes))
    }

    private fun setEndDate(stmt: Statement)
    {
        val dt = stmt.literal.value as XSDDateTime
        System.out.println("END " + dt.toString())
        event.changeEndDate(LocalDate.of(dt.years, dt.months, dt.days))
        event.changeEndTime(LocalTime.of(dt.hours, dt.minutes))
    }

    private fun setDuration(stmt: Statement)
    {
        val dt = stmt.literal.lexicalForm
        val duration = Duration.parse(dt.toString())
        println("-----------XXXXX ---------")
        println(duration)
        println(event.startTime)
        println(event.startTime.plus(duration))
        println(event.startDate)
        println("-----------XXXXX ---------")
        val endTime = event.startTime.plus(duration)
        event.changeEndTime(endTime)
        event.changeEndDate(event.startDate)
    }
}