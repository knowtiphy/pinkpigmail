package org.knowtiphy.pinkpigmail.util

import javafx.beans.value.ObservableValue
import org.knowtiphy.pinkpigmail.resources.Strings
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 *
 * @author graham
 */
object Format
{
    @JvmStatic
    fun format(p: ObservableValue<*>): String
    {
        return if (p.value == null) "" else p.value.toString()
    }

    @JvmStatic
    fun formatN(o: Any?): String
    {
        return o?.toString() ?: ""
    }

    @JvmStatic
    fun format(date: ZonedDateTime?): String
    {
        return when (date)
        {
            null -> ""
            ZonedDateTime.now() -> Strings.TODAY
            ZonedDateTime.now().minusDays(1) -> Strings.YESTERDAY
            else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }//	TODO -- settable formatting?
    }
}