package org.knowtiphy.pinkpigmail.calendarview

import com.calendarfx.model.Entry
import com.calendarfx.view.CalendarView
import com.calendarfx.view.DateControl
import com.calendarfx.view.Messages
import com.calendarfx.view.popover.EntryDetailsView
import com.calendarfx.view.popover.EntryHeaderView
import com.calendarfx.view.popover.PopOverContentPane
import com.calendarfx.view.popover.PopOverTitledPane
import java.util.*

//  don't need the PopOverContentPane -- regular old border pane should do
open class EditEntryDetails(dateControl : DateControl, entry : Entry<*>) : PopOverContentPane()
{
	private val entry : Entry<*>
	//private val dateControl : DateControl

	init
	{
		stylesheets.add(CalendarView::class.java.getResource("calendar.css").toExternalForm())
	//	this.dateControl = dateControl
		this.entry = Objects.requireNonNull(entry) as Entry<*>
		val details = EntryDetailsView(entry)
		val detailsPane = PopOverTitledPane(Messages.getString("Details"), details)
		header = EntryHeaderView(entry, dateControl.calendars)
		panes.addAll(detailsPane)
		expandedPane = detailsPane
	}
}

//		val listener = InvalidationListener { obs : Observable? ->
//			if (entry.isFullDay && !popOver.isDetached)
//			{
//				popOver.isDetached = true
//			}
//		}
//		entry.fullDayProperty().addListener(listener)
////		popOver.onHidden = EventHandler { evt : WindowEvent? ->
////			entry.fullDayProperty().removeListener(listener)
////		}
//		entry.calendarProperty().addListener { it : Observable? ->
//			if (entry.calendar == null)
//			{
//				popOver.hide(Duration.ZERO)
//			}
//		}