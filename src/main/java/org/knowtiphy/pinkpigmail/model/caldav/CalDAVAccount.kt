package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Calendar
import com.calendarfx.model.CalendarSource
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import org.apache.jena.arq.querybuilder.SelectBuilder
import org.apache.jena.graph.NodeFactory
import org.apache.jena.sparql.core.Var
import org.apache.jena.vocabulary.RDF
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.Globals
import org.knowtiphy.pinkpigmail.model.BaseAccount
import org.knowtiphy.pinkpigmail.model.ICalendarAccount
import org.knowtiphy.pinkpigmail.model.events.FinishSyncEvent
import org.knowtiphy.pinkpigmail.model.events.StartSyncEvent
import org.knowtiphy.pinkpigmail.model.storage.StorageEvent
import java.util.concurrent.Future

/**
 * @author graham
 */
class CalDAVAccount(id : String, storage : IStorage) : BaseAccount(id, Vocabulary.CALDAV_ACCOUNT, storage),
	ICalendarAccount
{
	companion object
	{
		val GET_CALENDAR_IDS : SelectBuilder =
			SelectBuilder().addVar("*").addWhere("?id", "<${Vocabulary.CONTAINS}>", "?cid")
				.addWhere("?cid", "<${RDF.type}>", "<${Vocabulary.CALDAV_CALENDAR}>")
	}

	override val nickNameProperty = SimpleStringProperty()
	override val emailAddressProperty = SimpleStringProperty()
	private val serverNameProperty = SimpleStringProperty()
	private val serverHeaderProperty = SimpleStringProperty()
	private val passwordProperty = SimpleStringProperty()

	private val calendars : ObservableMap<String, CalDAVCalendar> = FXCollections.observableHashMap()

	override val source = CalendarSource()

	init
	{
		declareU(Vocabulary.HAS_SERVER_NAME, serverNameProperty)
		declareU(Vocabulary.HAS_SERVER_HEADER, serverHeaderProperty)
		declareU(Vocabulary.HAS_EMAIL_ADDRESS, emailAddressProperty)
		declareU(Vocabulary.HAS_NICK_NAME, nickNameProperty)
		declareU(Vocabulary.HAS_PASSWORD, passwordProperty)

		eventHandlers[Vocabulary.ACCOUNT_SYNCED] = ::synced
	}

	override fun initialize()
	{
		//	initialize the attributes of this account
		initialize(attributes)

		GET_CALENDAR_IDS.setVar(Var.alloc("id"), NodeFactory.createURI(uri))
		storage.query(GET_CALENDAR_IDS.buildString()).forEach { addCalendar(it.getResource("cid").toString()) }
	}

	override fun sync() : Future<*>
	{
		Globals.push(StartSyncEvent(this, this))
		return super.sync()
	}

	@Suppress("UNUSED_PARAMETER")
	private fun synced(event : StorageEvent)
	{
		with(diff(Vocabulary.CALDAV_CALENDAR, calendars.keys)) {
			first.forEach(::addCalendar)
			second.forEach(::deleteCalendar)
			third.forEach(::updateCalendar)
		}

		Globals.push(FinishSyncEvent(this, this))
	}

	override fun getDefaultCalendar() : Calendar?
	{
		println(source.calendars)
		println(calendars)
		for (cal in calendars.values)
		{
			if(cal.calendar.name == "Calendar")
				return cal.calendar
		}
		return null
	}

	private fun addCalendar(cid : String)
	{
		println("ADD CAL " + cid)
		assert(!calendars.containsKey(cid))

		val calendar = CalDAVCalendar(cid, this, storage)
		calendar.initialize()

		//  TODO -- this is a little crappy since we create the inbox and then say nah!
		//  TODO -- what are these wierd ass inbox and outbox calendars
		if (calendar.calendar.name != "Outbox" && calendar.calendar.name != "Inbox")
		{
			calendars[cid] = calendar
			println("ADDING CAL TO SOURCE")
			source.calendars.add(calendar.calendar)
			println(source.calendars)
		}
	}

	private fun deleteCalendar(cid : String)
	{
		println("DEL CAL " + cid)
		//  TODO -- do we need to any more than this?
		assert(calendars.containsKey(cid)) { calendars }
		val calendar = calendars.remove(cid)
		source.calendars.remove(calendar!!.calendar)
	}

	private fun updateCalendar(cid : String)
	{
		println("UPDATE CAL " + cid)
		assert(calendars.containsKey(cid)) { calendars }
		calendars[cid]!!.initialize()
	}
}