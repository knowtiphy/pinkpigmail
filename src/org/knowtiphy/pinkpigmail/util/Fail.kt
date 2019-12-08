package org.knowtiphy.pinkpigmail.util

import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.later
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.maxSizeable
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.resizeable
import org.knowtiphy.utils.LoggerUtils
import java.util.logging.Level
import java.util.logging.Logger

/**
 *
 * @author graham
 */
object Fail
{
    fun failNoMessage(ex: Throwable)
    {
        Logger.getLogger(Fail::class.java.name).log(Level.SEVERE, null, ex)
    }

    fun fail(ex: Throwable)
    {
        failNoMessage(ex)

        later {
            val alert = Alert(AlertType.ERROR)
            with(alert) {
                width = 1600.0
                height = 1200.0
                title = Strings.APPLICATION_ERROR
                headerText = Strings.APPLICATION_ERROR_HAS_OCCURED
                contentText = ex.localizedMessage
            }

            val label = Label(Strings.STACKTRACE)

            val textArea = maxSizeable(TextArea(LoggerUtils.exceptionMessage(ex)))
            textArea.isEditable = false
            textArea.isWrapText = true

            val expContent = resizeable(GridPane())
            expContent.addColumn(0, label, textArea)
            GridPane.setHgrow(label, Priority.ALWAYS)
            GridPane.setVgrow(label, Priority.NEVER)
            GridPane.setVgrow(textArea, Priority.ALWAYS)
            GridPane.setHgrow(textArea, Priority.ALWAYS)

            alert.dialogPane.expandableContent = expContent
            alert.showAndWait()
        }
    }
}