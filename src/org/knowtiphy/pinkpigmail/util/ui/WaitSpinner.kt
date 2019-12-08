package org.knowtiphy.pinkpigmail.util.ui

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.VBox

class WaitSpinner(message : String): VBox()
{
    init
    {
        children.addAll(ProgressIndicator(), Label(message))
        spacing = 35.0
        alignment = Pos.CENTER
    }
}