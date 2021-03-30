package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Entry
import org.apache.jena.query.ParameterizedSparqlString
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.utils.JenaUtils.getDate

class CalDAVEvent(id : String, storage : IStorage) : StoredPeer(id, Vocabulary.CALDAV_EVENT, storage)
{
	companion object
	{
		val GET_ATTRIBUTES = ParameterizedSparqlString("select * where { ?s <" + Vocabulary.HAS_SUMMARY + "> ?o}")

		//  can an event have a start but no end? or any combination thereof?
		val GET_DATES =
			ParameterizedSparqlString("select * where { ?id <" + Vocabulary.HAS_DATE_START + "> ?start. ?id <" + Vocabulary.HAS_DATE_END + "> ?end }")
	}

	val event = Entry<String>("")

	init
	{
		declareU(Vocabulary.HAS_SUMMARY) { event.title = it.asLiteral().string }
	}

	fun initialize()
	{
		//	set the simple data properties of this message
		GET_ATTRIBUTES.setIri("s", id)
		initialize(storage.query(GET_ATTRIBUTES.toString()), Vocabulary.HAS_SUMMARY)

		//  have to handle start and end dates specially because calendarfx can't handle setting the end date before the start date
		GET_DATES.setIri("id", id)
		storage.query(GET_DATES.toString()).forEach { event.setInterval(getDate(it, "start"), getDate(it, "end")) }
	}
}