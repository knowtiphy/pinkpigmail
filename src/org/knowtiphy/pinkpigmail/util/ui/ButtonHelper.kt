package org.knowtiphy.pinkpigmail.util.ui

import javafx.scene.control.Button
import org.controlsfx.control.action.Action
import org.controlsfx.control.action.ActionUtils

/**
 *
 * @author graham
 */
object ButtonHelper
{
    const val STANDARD_BUTTON_STYLE_CLASS = "stdButton"

	fun button(action: Action, styleClass: String = STANDARD_BUTTON_STYLE_CLASS): Button
	{
		val b = ActionUtils.createButton(action)
		b.styleClass.add(styleClass)
		return b
	}

//	fun button(action: Action): Button
//	{
//		return button(action, STANDARD_BUTTON_STYLE_CLASS)
//	}
//
//	fun transparent(action: Action): Button
//	{
//		val b = ActionUtils.createButton(action)
//		//b.setStyle("-fx-border-style: none; -fx-border-width: 0; -fx-border-insets: 0;");
//		//b.setStyle("-fx-border-color:regular; -fx-background-radius:0");
//		b.background = Background.EMPTY
//		return b
//	}
}