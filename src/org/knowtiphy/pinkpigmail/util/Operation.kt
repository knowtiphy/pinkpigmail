package org.knowtiphy.pinkpigmail.util

/**
 * @author graham
 */
interface Operation
{
    companion object
    {
        fun perform(action: () -> Unit)
        {
            try
            {
                action.invoke()
            } catch (ex: Exception)
            {
                ex.printStackTrace(System.err)
                //Fail.fail(ex);
            }
        }
    }
}