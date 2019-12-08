package org.knowtiphy.pinkpigmail.util

import javafx.beans.value.ObservableValue
import org.knowtiphy.pinkpigmail.resources.Strings
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 *
 * @author graham
 */
object Format
{
    fun formatN(o: Any?) = o?.toString() ?: ""

    fun format(p: ObservableValue<*>) = formatN(p.value)

    //  TODO -- this is wrong -- needs fixing -- a zoned date time is almost never now, but we only want the date part
    fun format(date: ZonedDateTime?): String
    {
        return when (date!!.toLocalDate())
        {
            null -> ""
            LocalDate.now() -> Strings.TODAY
            LocalDate.now().minusDays(1) -> Strings.YESTERDAY
            else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }//	TODO -- settable formatting?
    }
}