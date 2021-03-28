package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.CalendarSource
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import org.apache.jena.arq.querybuilder.SelectBuilder
import org.apache.jena.graph.NodeFactory
import org.apache.jena.sparql.core.Var
import org.apache.jena.vocabulary.RDF
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.PinkPigMail
import org.knowtiphy.pinkpigmail.model.BaseAccount
import org.knowtiphy.pinkpigmail.model.ICalendarAccount
import org.knowtiphy.pinkpigmail.model.QueryHelper
import org.knowtiphy.pinkpigmail.model.events.AccountSyncDoneEvent
import org.knowtiphy.pinkpigmail.model.events.AccountSyncStartedEvent
import org.knowtiphy.pinkpigmail.model.storage.DavStorage
import org.knowtiphy.pinkpigmail.model.storage.StorageEvent

/**
 * @author graham
 */
class CalDAVAccount(id : String, storage : DavStorage) : BaseAccount<DavStorage>(id, storage), ICalendarAccount
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
		GET_CALENDAR_IDS.setVar(Var.alloc("id"), NodeFactory.createURI(id))
		storage.query(GET_CALENDAR_IDS.buildString()).forEach { addCalendar(it.getResource("cid").toString()) }
	}

	override fun sync()
	{
		PinkPigMail.pushEvent(AccountSyncStartedEvent(this))
		storage.sync(id)
	}

	@Suppress("UNUSED_PARAMETER")
	private fun synced(event : StorageEvent)
	{
		val t = QueryHelper.diff(storage.storage, GET_CALENDAR_IDS, id, "cid", calendars)
		t.first.forEach(::addCalendar)
		t.second.forEach(::deleteCalendar)
		t.third.forEach(::updateCalendar)

		PinkPigMail.pushEvent(AccountSyncDoneEvent(this))
	}

	private fun addCalendar(cid : String)
	{
		assert(!calendars.containsKey(cid))

		val calendar = CalDAVCalendar(cid, this, storage)
		calendar.initialize()

		//  TODO -- this is a little crappy since we create the inbox and then say nah!
		//  TODO -- what does this do?
		if (calendar.calendar.name != "Outbox" && calendar.calendar.name != "Inbox")
		{
			calendars[cid] = calendar
			source.calendars.add(calendar.calendar)
		}
	}

	private fun deleteCalendar(cid : String)
	{
		//  TODO -- do we need to any more than this?
		assert(calendars.containsKey(cid)) { calendars }
		val calendar = calendars.remove(cid)
		source.calendars.remove(calendar!!.calendar)
	}

	private fun updateCalendar(cid : String)
	{
		assert(calendars.containsKey(cid)) { calendars }
		calendars[cid]!!.initialize()
	}
}