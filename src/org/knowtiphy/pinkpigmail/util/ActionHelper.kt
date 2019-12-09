package org.knowtiphy.pinkpigmail.util

import javafx.event.ActionEvent
import javafx.scene.Node
import org.controlsfx.control.action.Action

/**
 * @author graham
 */
object ActionHelper
{
    private fun create(node: Node, handler: (ActionEvent) -> Unit, text: String?, tip: String, disabled: Boolean): Action
    {
        val action = Action(text) { handler.invoke(it) }
        with(action) {
            graphic = node
            longText = tip
            isDisabled = disabled
        }

        return action
    }

    fun create(node: Node, handler: (ActionEvent) -> Unit, tip: String, disabled: Boolean = true) = create(node, handler, null, tip, disabled)
    fun create(node: Node, handler: (ActionEvent) -> Unit, tip: String) = create(node, handler, null, tip, false)
}