package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Calendar
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.PeerState
import org.knowtiphy.owlorm.javafx.StoredPeer

class CalDAVCalendar(id: String, storage: IStorage) : StoredPeer(id, storage)
{
    val calendar = Calendar()

    init
    {
        declareU(Vocabulary.HAS_NAME) { calendar.name = it.literal.string }
        declareU(Vocabulary.CONTAINS, ::addEvent)
        declareD(Vocabulary.CONTAINS, ::deleteEvent)
        calendar.setStyle(Calendar.Style.STYLE1)
    }

    private fun addEvent(stmt: Statement)
    {
        System.out.println(stmt);
        (PeerState.peer(stmt.getObject().asResource())!! as CalDAVEvent).setCalendar(this);
    }

    private fun deleteEvent(stmt: Statement)
    {
        calendar.removeEntry((PeerState.peer(stmt.getObject().asResource()) as CalDAVEvent).event)
    }
}