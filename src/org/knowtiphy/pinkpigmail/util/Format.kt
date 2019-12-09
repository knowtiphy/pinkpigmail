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

    fun asDate(p: ObservableValue<*>) = formatN(p.value)

    fun asDate(zdt: ZonedDateTime?): String
    {
        return when (zdt!!.toLocalDate())
        {
            null -> ""
            LocalDate.now() -> Strings.TODAY
            LocalDate.now().minusDays(1) -> Strings.YESTERDAY
            else -> zdt.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }//	TODO -- settable formatting?
    }
}