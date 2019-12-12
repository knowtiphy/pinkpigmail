package org.knowtiphy.pinkpigmail.util.ui

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.VBox

class WaitSpinner(message : String): VBox()
{
    val progressIndicator = ProgressBar()

    init
    {
        val progress = ProgressBar()
        progress.progress = 1.0
        children.addAll(progress, Label(message))
        spacing = 35.0
        alignment = Pos.CENTER
    }
}