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
    private fun create(g: Node, handler: (ActionEvent) -> Unit, text: String?, tip: String, disabled: Boolean): Action
    {
        val action = Action(text) { e ->
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
            longText = tip
            isDisabled = disabled
        }

        return action
    }

    fun create(g: Node, handler: (ActionEvent) -> Unit, tip: String, disabled: Boolean = true) = create(g, handler, null, tip, disabled)
    fun create(g: Node, handler: (ActionEvent) -> Unit, tip: String) = create(g, handler, null, tip, false)
}