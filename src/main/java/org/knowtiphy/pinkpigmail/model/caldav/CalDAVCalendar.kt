package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Calendar
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import org.apache.jena.arq.querybuilder.SelectBuilder
import org.apache.jena.graph.NodeFactory
import org.apache.jena.sparql.core.Var
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.model.QueryHelper
import org.knowtiphy.pinkpigmail.model.storage.DavStorage

class CalDAVCalendar(id : String, val account : CalDAVAccount, storage : DavStorage) : StoredPeer<DavStorage>(id, storage)
{
	companion object
	{
		val CALENDAR_ATTRIBUTES : SelectBuilder =
			SelectBuilder().addVar("*").addWhere("?id", "?p", "?o").addFilter("?p != <${Vocabulary.CONTAINS}>")

		val EVENT_IDS_IN_CALENDAR : SelectBuilder =
			SelectBuilder().addVar("*").addWhere("?id", "<${Vocabulary.CONTAINS}>", "?eid")
	}

	private val events : ObservableMap<String, CalDAVEvent> = FXCollections.observableHashMap()

	val calendar = Calendar()

	init
	{
		declareU(Vocabulary.HAS_NAME) { calendar.name = it.literal.string }
		calendar.setStyle(Calendar.Style.STYLE2)
	}

	fun initialize()
	{
		//	initialize the data properties of this calendar
		CALENDAR_ATTRIBUTES.setVar(Var.alloc("id"), NodeFactory.createURI(id))
		initialize(storage.query(CALENDAR_ATTRIBUTES.buildString()))

		//  update the events in the folder
		val t = QueryHelper.diff(storage.storage, EVENT_IDS_IN_CALENDAR, id, "eid", events)
		t.first.forEach(::addEvent)
		t.second.forEach(::deleteEvent)
		t.third.forEach(::updateEvent)
	}

	private fun addEvent(eid : String)
	{
		assert(!events.containsKey(eid))

		val event = CalDAVEvent(eid, storage)
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
		assert(events.containsKey(eid)) { events }
		events[eid]!!.initialize()
	}
}