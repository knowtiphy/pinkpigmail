package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Calendar
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.PPPeer
import org.knowtiphy.pinkpigmail.model.imap.IMAPMessage

class CalDavCalendar(accountId: String, storage: IStorage) : PPPeer(accountId, storage)
{
    val calendar = Calendar()

    val nameProperty = SimpleStringProperty()

    init
    {
        nameProperty.bind(calendar.nameProperty())

        declareU(Vocabulary.HAS_NAME) { calendar.name = it.literal.string }
        declareU(Vocabulary.CONTAINS, ::addEvent)
        calendar.setStyle(Calendar.Style.STYLE2);
    }

    private fun addEvent(stmt: Statement)
    {
        val event = PEERS[stmt.getObject().toString()] as CalDavEvent
        event.calendar = calendar
        calendar.addEntry(event.event)
    }
}