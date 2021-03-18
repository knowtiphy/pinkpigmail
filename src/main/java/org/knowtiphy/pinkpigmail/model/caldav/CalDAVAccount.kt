package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.CalendarSource
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.vocabulary.RDF
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.PeerState
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.model.storage.DavStorage
import org.knowtiphy.pinkpigmail.model.ICalendarAccount
import org.knowtiphy.pinkpigmail.util.ui.StorageEvent
import org.knowtiphy.utils.JenaUtils

/**
 * @author graham
 */
class CalDAVAccount(id: String, storage: DavStorage) : StoredPeer<DavStorage>(id, storage), ICalendarAccount
{
    override val nickNameProperty = SimpleStringProperty()
    override val emailAddressProperty = SimpleStringProperty()
    private val serverNameProperty = SimpleStringProperty()
    private val serverHeaderProperty = SimpleStringProperty()
    private val passwordProperty = SimpleStringProperty()

    override val source = CalendarSource()

    init
    {
        declareU(Vocabulary.HAS_SERVER_NAME, serverNameProperty)
        declareU(Vocabulary.HAS_SERVER_HEADER, serverHeaderProperty)
        declareU(Vocabulary.HAS_EMAIL_ADDRESS, emailAddressProperty)
        declareU(Vocabulary.HAS_NICK_NAME, nickNameProperty)
        declareU(Vocabulary.HAS_PASSWORD, passwordProperty)
        declareU(Vocabulary.CONTAINS, ::addCalendar)
        declareD(Vocabulary.CONTAINS, ::deleteCalendar)
        emailAddressProperty.addListener { _: ObservableValue<out String?>, _: String?, newValue: String? ->
            if (nickNameProperty.get() == null)
                nickNameProperty.set(newValue)
        }
    }

    override fun handleEvent(event: StorageEvent)
    {
        println("IN EVENT HANDLER for CalDav")
        println("Event $event")
        JenaUtils.printModel(event.model, "Model $id")
    }

    override fun save(model: Model, name: Resource)
    {
        model.add(name, model.createProperty(RDF.type.toString()), model.createResource(Vocabulary.CALDAV_ACCOUNT))
        model.add(name, model.createProperty(Vocabulary.HAS_SERVER_NAME), serverNameProperty.get())
        model.add(name, model.createProperty(Vocabulary.HAS_SERVER_HEADER), serverHeaderProperty.get())
        model.add(name, model.createProperty(Vocabulary.HAS_EMAIL_ADDRESS), emailAddressProperty.get())
        model.add(name, model.createProperty(Vocabulary.HAS_PASSWORD), passwordProperty.get())
    }

    private fun addCalendar(stmt: Statement)
    {
        val calendar = PeerState.peer(stmt.getObject().asResource()) as CalDAVCalendar
        //  TODO should possibly do something better here
        if (calendar.calendar.name != "Outbox" && calendar.calendar.name != "Inbox")
        {
            source.calendars.add(calendar.calendar)
        }
    }

    private fun deleteCalendar(stmt: Statement)
    {
        source.calendars.remove((PeerState.peer(stmt.`object`.asResource()) as CalDAVCalendar).calendar)
    }
}