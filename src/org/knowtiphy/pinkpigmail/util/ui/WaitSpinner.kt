package org.knowtiphy.pinkpigmail.util.ui

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.VBox

class WaitSpinner(message: String) : VBox(35.0)
{
	private val progressIndicator = ProgressIndicator(1.0)

	init
	{
        children.addAll(progressIndicator, Label(message))
		alignment = Pos.CENTER
		visibleProperty().addListener {_, a, b -> println("VISIBILITY CHANGED XXXXXXXXXXXXXXXXXXXXXXXX")}
	}

    fun finish() : WaitSpinner
	{
		progressIndicator.progress = 1.0
        return this
	}

    fun resume(): WaitSpinner
	{
		progressIndicator.progress = -1.0
        return this
	}
}