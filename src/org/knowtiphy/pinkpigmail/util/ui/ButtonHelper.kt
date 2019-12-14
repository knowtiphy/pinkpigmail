package org.knowtiphy.pinkpigmail.util.ui

import javafx.scene.control.Button
import javafx.scene.layout.Background
import org.controlsfx.control.action.Action
import org.controlsfx.control.action.ActionUtils

/**
 *
 * @author graham
 */
object ButtonHelper
{
    fun regular(action: Action): Button
    {
        //b.setStyle("-fx-border-style: none; -fx-border-width: 0; -fx-border-insets: 0;");
        //b.setStyle("-fx-border-color:regular; -fx-background-radius:0");
        val b = ActionUtils.createButton(action)
        b.background = Background.EMPTY;
        return b
    }

    fun transparent(action: Action): Button
    {
        val b = ActionUtils.createButton(action)
        //b.setStyle("-fx-border-style: none; -fx-border-width: 0; -fx-border-insets: 0;");
        //b.setStyle("-fx-border-color:regular; -fx-background-radius:0");
        b.background = Background.EMPTY
        return b
    }
}