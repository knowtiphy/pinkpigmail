package org.knowtiphy.pinkpigmail.util.ui

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.VBox

class WaitSpinner(message : String) : VBox(35.0)
{
	private val progressIndicator = ProgressIndicator(1.0)

	init
	{
		children.addAll(progressIndicator, Label(message))
		alignment = Pos.CENTER

		//	if the spinner is added/removed to/from a scence, start/stop it
		sceneProperty().addListener { _, _, n -> if (n == null) finish() else resume() }
	}

	private fun finish() : WaitSpinner
	{
		println("STOPPING SPINNER $this")
		progressIndicator.progress = 1.0
		return this
	}

	private fun resume() : WaitSpinner
	{
		println("STARTING SPINNER $this")
		progressIndicator.progress = -1.0
		return this
	}
}