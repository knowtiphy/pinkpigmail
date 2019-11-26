package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Entry
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.PPPeer
import org.knowtiphy.utils.JenaUtils
import java.time.ZonedDateTime

class CalDAVEvent(accountId: String, storage: IStorage) : PPPeer(accountId, storage)
{
    val event = Entry<String>("")

    //  calendarfx can't handle setting the end date before the start date, so store them locally, and update when
    //  we have both
    var startDate: ZonedDateTime? = null
    var endDate: ZonedDateTime? = null

    init
    {
        declareU(Vocabulary.HAS_SUMMARY) { event.title = it.literal.string }
        declareU(Vocabulary.HAS_DATE_START, ::setStartDate)
        declareU(Vocabulary.HAS_DATE_END, ::setEndDate)
    }

    private fun update()
    {
        //  calendarfx can't handle setting the end date before the start date
        if (startDate != null && endDate != null)
        {
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
    }

    private fun setEndDate(stmt: Statement)
    {
        endDate = JenaUtils.getLDT(stmt.literal)
        update()
    }
}