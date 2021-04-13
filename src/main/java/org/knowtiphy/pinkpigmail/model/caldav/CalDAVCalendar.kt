package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Calendar
import com.calendarfx.model.CalendarEvent
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.event.EventHandler
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer

class CalDAVCalendar(id : String, val account : CalDAVAccount, storage : IStorage) :
	StoredPeer(id, Vocabulary.CALDAV_CALENDAR, storage)
{
	companion object
	{
		//  indicates that an add/delete came from Babbage and not from the UI
		//  you need this to avoid an infinite loop -- Babbage syncs, finds a new entry, adds it to
		//  the cache, notifies the client of the new event, it is seen here via addEvent(), which adds an
		//  entry to the calendar event lists, which triggers a call into the CalendarFX event handler handle(),
		//  which thinks its a new event, so sends it Babbage, ad infinitum
		//  you could work this out from the events map (the underlying CalendarFX caldav event would/would not
		//  be in the events entry of some event in the values of the events map), but that's expensive.
		//  hence this hack! :)
		private const val FROM_BABBAGE = "1"
	}

	private val events : ObservableMap<String, CalDAVEvent> = FXCollections.observableHashMap()

	val calendar = Calendar()

	init
	{
		declareU(Vocabulary.HAS_NAME) { calendar.name = it.asLiteral().string }
		calendar.setStyle(Calendar.Style.STYLE1)
		calendar.addEventHandler(EventHandler(::handle))
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

	//  handles CalendarFX events
	private fun handle(event : CalendarEvent)
	{
		if (event.isEntryAdded)
		{
//			println("ADDING AN EVENT")
//			println(event.entry.userObject == FROM_BABBAGE)
//			println(event.entry.userObject == null)
		}

//		when (event.eventType)
//		{
//			CalendarEvent.CALENDAR_CHANGED -> println("CALENDAR_CHANGED")
//			CalendarEvent.ENTRY_CHANGED -> println("ENTRY CHANGED")
//			CalendarEvent.ENTRY_CALENDAR_CHANGED -> println("ENTRY_CALENDAR_CHANGED")
//			CalendarEvent.ENTRY_FULL_DAY_CHANGED -> println("ENTRY_FULL_DAY_CHANGED")
//			CalendarEvent.ENTRY_RECURRENCE_RULE_CHANGED -> println("ENTRY_RECURRENCE_RULE_CHANGED")
//			CalendarEvent.ENTRY_USER_OBJECT_CHANGED -> println("ENTRY_USER_OBJECT_CHANGED")
//			CalendarEvent.ENTRY_LOCATION_CHANGED -> println("ENTRY_LOCATION_CHANGED")
//			CalendarEvent.ENTRY_INTERVAL_CHANGED -> println("ENTRY_INTERVAL_CHANGED")
//			else -> println("SOMETHING ELSE " + event.eventType)
//		}
	}

	private fun addEvent(eid : String)
	{
		assert(!events.containsKey(eid))
		val event = CalDAVEvent(eid, storage)
		//  the add came from Babbage not the UI
		event.event.userObjectProperty().set(FROM_BABBAGE)
		event.initialize()
		events[eid] = event
		calendar.addEntry(event.event)
	}

	private fun deleteEvent(eid : String)
	{
		//  TODO -- do we need to any more than this?
		assert(events.containsKey(eid)) { events }
		val event = events.remove(eid)
		calendar.removeEntry(event!!.event)
	}

	private fun updateEvent(eid : String)
	{
		//println("UPDATE AVENT " + eid)
		assert(events.containsKey(eid)) { events }
		events[eid]!!.initialize()
	}
}