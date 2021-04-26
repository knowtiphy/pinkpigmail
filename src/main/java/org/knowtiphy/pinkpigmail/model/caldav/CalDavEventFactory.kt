package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.Entry
import com.calendarfx.view.AllDayView
import com.calendarfx.view.DateControl
import javafx.util.Callback
import org.knowtiphy.babbage.storage.IStorage
import java.time.Duration

class CalDavEventFactory(private val storage : IStorage):Callback<DateControl.CreateEntryParameter, Entry<*>>
{
	override fun call(param : DateControl.CreateEntryParameter) : Entry<*>
	{
		val control = param.dateControl
		val grid = control.virtualGrid
		var time = param.zonedDateTime
		val firstDayOfWeek = control.firstDayOfWeek

		val lowerTime = grid.adjustTime(time, false, firstDayOfWeek)
		val upperTime = grid.adjustTime(time, true, firstDayOfWeek)

		if (Duration.between(time, lowerTime).abs().minus(Duration.between(time, upperTime).abs()).isNegative) {
			time = lowerTime
		} else {
			time = upperTime
		}

		//  TODO -- this is a hack -- no URI will match ""
		val entry =  CalDAVEvent("", storage, EventState.NEW)
		entry.changeStartDate(time.toLocalDate())
		entry.changeStartTime(time.toLocalTime())
		entry.changeEndDate(entry.startDate)
		entry.changeEndTime(entry.startTime.plusHours(1))

		if (control is AllDayView) {
			entry.isFullDay = true
		}

		entry.userObject = EventState.NEW
		println("CREATED ENTRY " + entry)
		println("CREATED ENTRY " + entry.userObject)
		return entry
	}
}