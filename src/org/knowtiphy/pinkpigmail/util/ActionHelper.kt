package org.knowtiphy.pinkpigmail.util

import javafx.event.ActionEvent
import javafx.scene.Node
import org.controlsfx.control.action.Action
import org.knowtiphy.pinkpigmail.Fail

/**
 * @author graham
 */
object ActionHelper
{
    private fun create(g: Node, handler: (ActionEvent) -> Unit, txt: String?, tip: String, disabled: Boolean): Action
    {
        val action = Action(null) { e ->
            try
            {
                handler.invoke(e)
            } catch (ex: Exception)
            {
                Fail.fail(ex)
            }
        }

        with(action) {
            graphic = g
            text = txt
            longText = tip
            isDisabled = disabled
        }

        return action
    }

    fun create(g: Node, handler: (ActionEvent) -> Unit, tip: String, disabled: Boolean = true): Action
    {
        return create(g, handler, null, tip, disabled)
    }

    fun create(g: Node, handler: (ActionEvent) -> Unit, tip: String): Action
    {
        return create(g, handler, null, tip, false)
    }
}