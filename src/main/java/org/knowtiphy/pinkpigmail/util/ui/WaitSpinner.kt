package org.knowtiphy.pinkpigmail.util.ui

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.VBox

class WaitSpinner(message : String, val indicator : ProgressIndicator = ProgressIndicator(1.0)) : VBox(35.0)
{
	init
	{
		children.addAll(indicator, Label(message))
		alignment = Pos.CENTER
		//	if the spinner is added/removed to/from a scence, start/stop it
		sceneProperty().addListener { _, _, n -> indicator.progress = if (n == null) 1.0 else -1.0 }
	}
}