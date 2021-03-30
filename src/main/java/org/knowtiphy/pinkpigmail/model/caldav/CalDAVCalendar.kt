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
	private val events : ObservableMap<String, CalDAVEvent> = FXCollections.observableHashMap()

	val calendar = Calendar()

	init
	{
		declareU(Vocabulary.HAS_NAME) { calendar.name = it.asLiteral().string }
		calendar.setStyle(Calendar.Style.STYLE1)

		//calendar.addEventHandler(CalendarEvent.ANY) {  }

		val handler : EventHandler<CalendarEvent> = EventHandler<CalendarEvent> { evt -> foo(evt) }
		calendar.addEventHandler(handler)
	}

	fun foo(event : CalendarEvent)
	{
		println("ADD EVENT $event")
		//println(events.containsKey(it.))
		//when(it.t)
		if (event.isEntryAdded)
		{
			println("ADDING AN EVENT")
		}

		when (event.eventType)
		{
			CalendarEvent.ENTRY_CHANGED -> println("ENTRY CHANGED")
			else -> println("SOMETHING ELSE " + event.eventType)
		}

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

	private fun addEvent(eid : String)
	{
		//println("ADD AVENT " + eid)
		assert(!events.containsKey(eid))

		val event = CalDAVEvent(eid, storage)
		event.initialize()

		events[eid] = event
		calendar.addEntry(event.event)
	}

	private fun deleteEvent(eid : String)
	{
		//println("DELETE AVENT " + eid)
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