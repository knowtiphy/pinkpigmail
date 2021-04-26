package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Entry
import org.apache.jena.query.ParameterizedSparqlString
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.IStoredPeer
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.utils.JenaUtils.getDT

//  EventState.Babbage indicates that an add/delete came from Babbage and not from the UI
//  you need this to avoid an infinite loop -- Babbage syncs, finds a new entry, adds it to
//  the cache, notifies the client of the new event, it is seen here via addEvent(), which adds an
//  entry to the calendar event lists, which triggers a call into the CalendarFX event handler handle(),
//  which thinks its a new event, so sends it Babbage, ad infinitum
//  you could work this out from the events map (the underlying CalendarFX caldav event would/would not
//  be in the events entry of some event in the values of the events map), but that's expensive.
//  hence this hack! :)
//  TODO -- none of this fiddling with event states is necessary if we can make event creation and editing modal

class CalDAVEvent(uri : String, storage : IStorage, kind : EventState = EventState.BABBAGE) : Entry<EventState>(),
	IStoredPeer by StoredPeer(uri, Vocabulary.CALDAV_EVENT, storage)
{
	companion object
	{
		//  can an event have a start but no end? or any combination thereof?
		// @formatter:off
		val GET_DATES = ParameterizedSparqlString(
			"select * " + "where { " +
					"?id <" + Vocabulary.HAS_DATE_START + "> ?start. " +
					"?id <" + Vocabulary.HAS_DATE_END + "> ?end }"
		)
		// @formatter:on
	}

	init
	{
		userObject = kind
		addUpdater(Vocabulary.HAS_SUMMARY) { title = it.asLiteral().string }
	}

	fun initialize()
	{
		//	set the simple data properties of this message
		initialize(attributes)

		//  handle start and end dates specially because Calendarfx can't handle setting the end date before the start date
		GET_DATES.setIri("id", uri)
		storage.query(GET_DATES.toString()).forEach { setInterval(getDT(it, "start"), getDT(it, "end")) }
	}
}