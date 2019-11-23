package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Calendar
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.PPPeer

class CalDAVCalendar(accountId: String, storage: IStorage) : PPPeer(accountId, storage)
{
    val calendar = Calendar()

    init
    {
        declareU(Vocabulary.HAS_NAME) { calendar.name = it.literal.string }
        declareU(Vocabulary.CONTAINS, ::addEvent)
        calendar.setStyle(Calendar.Style.STYLE2)
    }

    private fun addEvent(stmt: Statement)
    {
        val event = PEERS[stmt.getObject().toString()] as CalDAVEvent
        event.calendar = calendar
        calendar.addEntry(event.event)
    }
}