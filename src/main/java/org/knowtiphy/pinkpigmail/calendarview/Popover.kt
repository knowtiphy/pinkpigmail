package org.knowtiphy.pinkpigmail.calendarview

import com.calendarfx.view.DateControl
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.layout.Region
import javafx.util.Callback
import org.knowtiphy.pinkpigmail.model.caldav.EventState
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.resizeable
import java.util.*

class Popover : Callback<DateControl.EntryDetailsParameter, Boolean>
{
	//  Note: I am sure some formula on the event states tells us whether we should biff the event
	//  but for the moment the code here will do
	override fun call(param : DateControl.EntryDetailsParameter) : Boolean
	{
		val oldState = param.entry.userObject
		param.entry.userObject = EventState.EDITING

		val dialog = Alert(Alert.AlertType.CONFIRMATION).apply {
			dialogPane.content = resizeable(EditEntryDetails(param.dateControl, param.entry))
			isResizable = true
		}

		dialog.dialogPane.minWidth(Region.USE_PREF_SIZE)

		val result : Optional<ButtonType> = dialog.showAndWait()
		return if (result.isPresent && result.get() === ButtonType.OK)
		{
			param.entry.userObject = EventState.SAVED
			//  keep the event
			true
		} else
		{
			param.entry.userObject = oldState
			//  delete the event if it's a new event that they edited the details of and then cancelled
			println(oldState)
			println(oldState == EventState.NEW)
			oldState != EventState.NEW
		}
	}
}

//val model = EntryModel(entry)
//println("FFFFFFF")
//model.getFormInstance()!!.persist()

//			with(entry) {
//				title = model.title.get()
//				if (location != null) location = model.location.get()
//				changeStartDate(model.startDateP.get().toLocalDate())
//				changeStartTime(model.startDateP.get().toLocalTime())
//				changeEndDate(model.endDateP.get().toLocalDate())
//				changeEndTime(model.endDateP.get().toLocalTime())
//				userObject = EventState.SAVED
//
////				val cal = entry.calendar
////				println(cal)
////				entry.calendar.removeEntry(entry)
////				cal.addEntry(entry)
//			}
