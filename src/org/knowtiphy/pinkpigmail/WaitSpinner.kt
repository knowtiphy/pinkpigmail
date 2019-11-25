package org.knowtiphy.pinkpigmail

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.VBox

class WaitSpinner : VBox()
{
    init
    {
        val progress = ProgressIndicator()
        val message = Label("Synchronizing Accounts -- Please Wait")
        children.addAll(progress, message)
        spacing = 25.0
        alignment = Pos.CENTER
    }
}