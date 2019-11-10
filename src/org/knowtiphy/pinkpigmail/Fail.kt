package org.knowtiphy.pinkpigmail

import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import org.knowtiphy.pinkpigmail.resources.Strings

import java.io.PrintWriter
import java.io.StringWriter
import java.util.logging.Level
import java.util.logging.Logger

/**
 *
 * @author graham
 */
object Fail
{
    @JvmStatic
    fun failNoMessage(ex: Throwable)
    {
        Logger.getLogger(Fail::class.java.name).log(Level.SEVERE, null, ex)
    }

    @JvmStatic
    fun fail(ex: Throwable)
    {
        System.err.println("Application Failure")
        failNoMessage(ex)

        Platform.runLater {
            val alert = Alert(AlertType.ERROR)
            alert.width = 1600.0
            alert.height = 1200.0
            alert.title = Strings.APPLICATION_ERROR
            alert.headerText = Strings.APPLICATION_ERROR_HAS_OCCURED
            alert.contentText = ex.localizedMessage

            val sw = StringWriter()
            val pw = PrintWriter(sw)
            ex.printStackTrace(pw)
            val exceptionText = sw.toString()

            val label = Label(Strings.STACKTRACE)

            val textArea = TextArea(exceptionText)
            textArea.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
            textArea.isEditable = false
            textArea.isWrapText = true

            val expContent = GridPane()
            expContent.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
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