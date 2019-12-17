package org.knowtiphy.pinkpigmail.util.ui

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.VBox

class WaitSpinner(message : String): VBox(35.0)
{
    private val progressIndicator = ProgressBar()

    init
    {
        progressIndicator.progress = 1.0
        children.addAll(progressIndicator, Label(message))
        alignment = Pos.CENTER
    }
}