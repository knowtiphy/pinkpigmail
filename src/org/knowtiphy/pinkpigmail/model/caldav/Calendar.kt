package org.knowtiphy.pinkpigmail.model.caldav

import javafx.beans.property.SimpleStringProperty
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.PPPeer

class Calendar(accountId: String, storage: IStorage) : PPPeer(accountId, storage)
{
    val calendar = com.calendarfx.model.Calendar()

    val nameProperty = SimpleStringProperty()

    init
    {
        nameProperty.bind(calendar.nameProperty())

        declareU(Vocabulary.HAS_NAME) { calendar.name = it.literal.string }
        declareU(Vocabulary.CONTAINS, ::addEvent)
        calendar.setStyle(com.calendarfx.model.Calendar.Style.STYLE2);
    }

    private fun addEvent(stmt: Statement)
    {
        val event = PEERS[stmt.getObject().toString()] as Event
        event.calendar = calendar
        calendar.addEntry(event.event)
    }
}