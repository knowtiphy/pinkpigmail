package org.knowtiphy.pinkpigmail

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.VBox

class WaitSpinner(message : String): VBox()
{
    init
    {
        val progress = ProgressIndicator()
        val message = Label(message)
        children.addAll(progress, message)
        spacing = 35.0
        alignment = Pos.CENTER
    }
}