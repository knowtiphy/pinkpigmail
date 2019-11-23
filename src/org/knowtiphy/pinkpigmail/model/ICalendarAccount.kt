package org.knowtiphy.pinkpigmail.model

import com.calendarfx.model.CalendarSource
import org.knowtiphy.pinkpigmail.ITreeItem

interface ICalendarAccount : ITreeItem, IAccount
{
    val source: CalendarSource
}