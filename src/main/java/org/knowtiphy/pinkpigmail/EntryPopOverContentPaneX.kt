package org.knowtiphy.pinkpigmail

import com.calendarfx.model.Entry
import com.calendarfx.view.DateControl
import com.calendarfx.view.Messages
import com.calendarfx.view.popover.EntryDetailsView
import com.calendarfx.view.popover.EntryPopOverContentPane
import com.calendarfx.view.popover.PopOverTitledPane
import javafx.scene.control.Label
import org.controlsfx.control.PopOver

class EntryPopOverContentPaneX(popOver : PopOver, dateControl  : DateControl, entry : Entry<*>) :
	EntryPopOverContentPane(popOver, dateControl, entry)
{
		init{
			val details = Label("XXXXX")
			val detailsPane = PopOverTitledPane(Messages.getString("EntryPopOverContentPane.FOO"), details)

			panes.add(detailsPane)
		}
	}