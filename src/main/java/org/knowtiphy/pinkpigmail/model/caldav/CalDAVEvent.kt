package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Entry
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.model.storage.DavStorage
import org.knowtiphy.utils.JenaUtils
import java.time.ZonedDateTime

class CalDAVEvent(id: String, storage: DavStorage) : StoredPeer<DavStorage>(id, storage)
{
    val event = Entry<String>("")

    //  calendarfx can't handle setting the end date before the start date, so store them locally, and update as
    //  an interval when we have both
    private var startDate: ZonedDateTime? = null
    private var endDate: ZonedDateTime? = null

    init
    {
        declareU(Vocabulary.HAS_SUMMARY) { event.title = it.literal.string }
        declareU(Vocabulary.HAS_DATE_START, ::setStartDate)
        declareU(Vocabulary.HAS_DATE_END, ::setEndDate)
    }

    private fun update()
    {
        //  calendarfx can't handle setting the end date before the start date
        //  it also can't handle adding an event with no date
        if (startDate != null && endDate != null)
        {
            event.setInterval(startDate, endDate)
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