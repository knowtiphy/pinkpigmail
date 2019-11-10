package org.knowtiphy.pinkpigmail

import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.util.UIUtils
import java.util.*

/**
 *
 * @author graham
 */
object Comparators
{
    fun <T : Comparable<T>> cmp(e: (IMessage) -> T): Comparator<IMessage>
    {
        return UIUtils.comparator { x: IMessage, y: IMessage ->
            try
            {
                e.invoke(x).compareTo(e.invoke(y))
            } catch (ex: Exception)
            {
                ex.printStackTrace()
                0
            }
        }
    }
//
//    internal fun <T : Comparable<T>> cmp(): Comparator<T>
//    {
//        return UIUtils.comparator { x: T, y: T ->
//            try
//            {
//                x.compareTo(y)
//            } catch (ex: Exception)
//            {
//                ex.printStackTrace()
//                0
//            }
//        }
//    }
}