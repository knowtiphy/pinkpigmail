package org.knowtiphy.pinkpigmail.model

import com.calendarfx.model.Calendar
import com.calendarfx.model.CalendarSource
import javafx.beans.property.StringProperty

interface ICalendarAccount : IAccount
{
    val emailAddressProperty: StringProperty

    val source: CalendarSource

    fun getDefaultCalendar() : Calendar?
}