package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Calendar
import com.calendarfx.model.CalendarEvent
import com.calendarfx.model.Entry
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.event.EventHandler
import org.apache.jena.rdf.model.ModelFactory
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.IStoredPeer
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.Globals
import org.knowtiphy.utils.JenaUtils

class CalDAVCalendar(id : String, val account : CalDAVAccount, storage : IStorage) : Calendar(),
	IStoredPeer by StoredPeer(id, Vocabulary.CALDAV_CALENDAR, storage)
{
	private val events : ObservableMap<String, CalDAVEvent> = FXCollections.observableHashMap()

	init
	{
		addUpdater(Vocabulary.HAS_NAME) { name = it.asLiteral().string }
		setStyle(Style.STYLE1)

		addEventHandler(EventHandler(::handleUIEvents))
	}

	fun initialize()
	{
		//	initialize the data properties of this calendar
		initialize(attributes)

		//  update the events in the folder
		with(diff(Vocabulary.CALDAV_EVENT, events.keys)) {
			first.forEach(::addEvent)
			second.forEach(::deleteEvent)
			third.forEach(::updateEvent)
		}
	}

	//  handles CalendarFX events -- so user interface driven events
	private fun handleUIEvents(event : CalendarEvent)
	{
//		if (event.isEntryAdded)
//		{
//			println("UI ADDING AN EVENT")
//			println(event.oldUserObject)
//			println(event.entry.userObject)
//		}

		when (event.eventType)
		{
			CalendarEvent.ENTRY_USER_OBJECT_CHANGED -> if (event.entry.userObject == EventState.SAVED) toServer(event.entry)
	//		else -> println("SOMETHING ELSE " + event.eventType + " " + event.entry.userObject)
		}
	}

	//  add an event due to the storage layer informing us of a new event
	private fun addEvent(eid : String)
	{
		assert(!events.containsKey(eid))
		val entry = CalDAVEvent(eid, storage)
		entry.initialize()
		events[eid] = entry
		addEntry(entry)
	}

	//  delete an event due to the storage layer informing us of a new event
	private fun deleteEvent(eid : String)
	{
		//  TODO -- do we need to any more than this?
		assert(events.containsKey(eid)) { events }
		val event = events.remove(eid)
		removeEntry(event)
	}

	private fun updateEvent(eid : String)
	{
		//println("UPDATE AVENT " + eid)
		assert(events.containsKey(eid)) { events }
		events[eid]!!.initialize()
	}

	private fun toServer(entry : Entry<*>)
	{
		println("SAVING TO CALDAV")
		println(entry)

		val mid = Globals.nameSource.get()
		val opId = Globals.nameSource.get()

		val operation = ModelFactory.createDefaultModel()

		JenaUtils.addType(operation, opId, Vocabulary.ADD_CALDAV_EVENT)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_ACCOUNT, account.uri)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_CALENDAR, uri)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_EVENT, mid)

		JenaUtils.addDPN(operation, mid, Vocabulary.HAS_SUMMARY, entry.title)
		JenaUtils.addDPN(operation, mid, Vocabulary.HAS_LOCATION, entry.location)
		JenaUtils.addDPN(operation, mid, Vocabulary.HAS_DATE_START, entry.startAsZonedDateTime)
		JenaUtils.addDPN(operation, mid, Vocabulary.HAS_DATE_END, entry.endAsZonedDateTime)

		storage.doOperation(operation)
	}
}

//			CalendarEvent.CALENDAR_CHANGED -> println("CALENDAR_CHANGED")
//			CalendarEvent.ENTRY_CHANGED -> println("ENTRY CHANGED")
//			CalendarEvent.ENTRY_CALENDAR_CHANGED -> println("ENTRY_CALENDAR_CHANGED")
//			CalendarEvent.ENTRY_FULL_DAY_CHANGED -> println("ENTRY_FULL_DAY_CHANGED")
//			CalendarEvent.ENTRY_RECURRENCE_RULE_CHANGED -> println("ENTRY_RECURRENCE_RULE_CHANGED")
//			CalendarEvent.ENTRY_LOCATION_CHANGED -> println("ENTRY_LOCATION_CHANGED")
//			CalendarEvent.ENTRY_INTERVAL_CHANGED -> println("ENTRY_INTERVAL_CHANGED")


//		if (event.isEntryRemoved)
//		{
//			println("UI REMOVING AN EVENT")
//			println(event)
//			println(event.entry.userObject == FROM_BABBAGE)
//			println(event.entry.userObject == null)
//		}


//		if (event.entry.userObject == EventState.SAVED)
//		{
//			println("SAVED")
//			toServer(event.entry)
//		} else
//		{
//			println("CANCELLED")
//			event.entry.calendar.removeEntry(event.entry)
//		}