package org.knowtiphy.pinkpigmail.model

import com.calendarfx.model.CalendarSource
import javafx.beans.property.StringProperty
import org.knowtiphy.pinkpigmail.ITreeItem

interface ICalendarAccount : ITreeItem, IAccount
{
    val emailAddressProperty: StringProperty

    val source: CalendarSource
}