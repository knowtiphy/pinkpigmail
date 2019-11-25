package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Entry
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.PPPeer
import org.knowtiphy.utils.JenaUtils
import java.time.LocalDateTime

class CalDAVEvent(accountId: String, storage: IStorage) : PPPeer(accountId, storage)
{
    val event = Entry<String>("")

    var startDate: LocalDateTime? = null
    var endDate: LocalDateTime? = null

    init
    {
        declareU(Vocabulary.HAS_SUMMARY) { event.title = it.literal.string }
        declareU(Vocabulary.HAS_DATE_START, ::setStartDate)
        declareU(Vocabulary.HAS_DATE_END, ::setEndDate)
    }

    private fun update()
    {
        if (startDate != null && endDate != null)
        {
            println("SET EVENT TIME " + id + " :: " + startDate + "::" +endDate)
            event.changeStartDate(startDate!!.toLocalDate())
            event.changeStartTime(startDate!!.toLocalTime())
            event.changeEndDate(endDate!!.toLocalDate())
            event.changeEndTime(endDate!!.toLocalTime())
        }
    }

    private fun setStartDate(stmt: Statement)
    {
        startDate = JenaUtils.getLDT(stmt.literal)
        update()
//        println("START DATE = "+ dt)
//        println("START DATE = "+ dt.toLocalDate())
//        println("START DATE = "+ dt.toLocalTime())
//        event.changeStartDate(dt.toLocalDate())
//        event.changeStartTime(dt.toLocalTime())
    }

    private fun setEndDate(stmt: Statement)
    {
        endDate = JenaUtils.getLDT(stmt.literal)
        update()
//        println("END DATE = "+ dt)
//        println("END DATE = "+ dt.toLocalDate())
//        println("END TIME = "+ dt.toLocalTime())
//event.changeEndDate(dt.toLocalDate())
//        event.changeEndTime(dt.toLocalTime())
    }
}