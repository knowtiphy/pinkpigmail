package org.knowtiphy.pinkpigmail

import org.knowtiphy.pinkpigmail.model.IMessage
import java.util.*

/**
 *
 * @author graham
 */
object Comparators
{
    fun <T : Comparable<T>> cmp(e: (IMessage) -> T?): Comparator<IMessage>
    {
        return kotlin.Comparator { x, y ->
            val a = e.invoke(x)
            val b = e.invoke(y)
            val result = if (a == null) if (b == null) 0 else 1 else if (b == null) -1 else a.compareTo(b)
//            println((a?.toString() ?: "NULL") + " : " + (b?.toString() ?: "NULL") + " : " + result)
            result
        }
    }
}