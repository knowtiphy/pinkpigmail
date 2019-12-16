package org.knowtiphy.pinkpigmail

import javafx.event.ActionEvent
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.SplitMenuButton
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.input.KeyEvent
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.web.HTMLEditor
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.knowtiphy.babbage.storage.StorageException
import org.knowtiphy.pinkpigmail.model.EmailSendMode
import org.knowtiphy.pinkpigmail.model.IMessageModel
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.ActionHelper
import org.knowtiphy.pinkpigmail.util.ui.ButtonHelper
import org.knowtiphy.pinkpigmail.util.ui.UIUtils
import tornadofx.Dimension
import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author graham
 */
object ComposeMessage
{
    private val INSETS = Insets(2.0, 2.0, 2.0, 3.0)

    private fun header(model: IMessageModel): GridPane
    {
        val subject = Label(Strings.SUBJECT)
        val cc = Label(Strings.CC)
        val to = Label(Strings.TO)

        val subjectField = TextField(model.subjectProperty().get())
        model.subjectProperty().bindBidirectional(subjectField.textProperty())
        val toField = TextField(model.toProperty().get())
        model.toProperty().bindBidirectional(toField.textProperty())
        val ccField = TextField(model.ccProperty().get())
        model.ccProperty().bindBidirectional(ccField.textProperty())

        val grid = GridPane()
        grid.hgap = 30.0
        grid.vgap = 3.0

        grid.addColumn(0, to, subject, cc)
        grid.addColumn(1, toField, subjectField, ccField)

        GridPane.setHgrow(subjectField, Priority.ALWAYS)
        GridPane.setHgrow(toField, Priority.ALWAYS)
        GridPane.setHgrow(ccField, Priority.ALWAYS)

        return grid
    }

    private fun attachFile(stage: Stage): List<File>?
    {
        val fileChooser = FileChooser()
        fileChooser.title = Strings.ATTACH_FILE
        return fileChooser.showOpenMultipleDialog(stage)
    }

    private fun foo(ke: KeyEvent)
    {
        println("START EVENT")
        println(ke.character)
        println(ke.code)
        println(ke.isAltDown)
        println(ke.isControlDown)
        println(ke.isMetaDown)
        println(ke.isShiftDown)
        println(ke.isShiftDown)
        println(ke.text)
        println(ke.source)
        println(ke.target)
        println(ke.eventType)
        println("END EVENT")
    }

    fun compose(model: IMessageModel, send: (IMessageModel) -> Unit)
    {
        val stage = UIUtils.getStage(Strings.FROM + " : " + model.mailAccount.emailAddressProperty.get(), 800.0, 700.0)

        val toolBar = HBox()
        val root = VBox(header(model), toolBar)

        val scene = Scene(root)

        var sendAction: ((ActionEvent) -> Unit)? = null
        var saveAction: ((ActionEvent) -> Unit)? = null

        when (model.sendMode)
        {
            EmailSendMode.TEXT ->
            {
                val editor = TextArea()
                editor.text = model.contentProperty().get()
                model.contentProperty().bind(editor.textProperty())
                editor.positionCaret(1)
                VBox.setVgrow(editor, Priority.ALWAYS)
                root.children.add(editor)
                sendAction = {
                    stage.close()
                    send.invoke(model)
                }
                saveAction = {
                    try
                    {
                        model.saveToDrafts()
                    }
                    catch (ex: StorageException)
                    {
                        Logger.getLogger(ComposeMessage::class.java.name).log(Level.SEVERE, null, ex)
                    }
                }
            }
            EmailSendMode.HTML ->
            {
                val editor = HTMLEditor()
                editor.htmlText = model.contentProperty().get()
                // Define an event filter
//                val filter = EventHandler<KeyEvent>() {
//                    // public void handle(InputEvent event) {
//                    System.out.println("Filtering out event " + it.getEventType());
//                    println("ORIGINAL")
//                    foo(it)
//                    println(":" + it.character + ":")
//                    println(":" + it.character.length + ":")
//                    println(":" + (it.character.equals(KeyCode.CONTROL.toString())) + ":")
//
//                    if(it.code == KeyCode.M && it.isControlDown)
//                    {
//                        println("CCCC IGNORE IT")
//                        it.consume()
//                    }
//
//                    if (it.code == KeyCode.ENTER)
//                    {
//                        //  pretty sure this is the right key-event
//                        val ke = KeyEvent(it.source, it.target, it.eventType,
//                                KeyCode.CONTROL.toString(), "", KeyCode.M, false, true, false, false);
//                        println("TRANSFORMED");
//                        foo(ke);
//                        later { root.scene.processKeyEvent(ke) }
//                        it.consume();
//                    }
//                }
//                editor.addEventFilter(KeyEvent.KEY_PRESSED, filter);
//                editor.setOnKeyPressed { evt ->
//
//                    println("ORIGINAL")
//                    foo(evt)
//                    if (evt.code == KeyCode.ENTER)
//                    {
//                        //editor.fireEvent(new KeyEvent());
//                        val ke = KeyEvent(evt.source, evt.target, evt.eventType,
//                                null, null, KeyCode.M, false, true, false, false);
//                        println("TRANSFORMED");
//                        foo(ke);
//                        root.scene.eventDispatcher.dispatchEvent(ke)
//                        ///editor.setHtmlText(editor.getHtmlText() + "</p><p></p>");
//                    }
//                }
                //	TODO -- initialize the insert cursor at the beginning?
                VBox.setVgrow(editor, Priority.ALWAYS)
                root.children.add(editor)
                sendAction = {
                    stage.close()
                    model.contentProperty().set(editor.htmlText)
                    send.invoke(model)
                }
                saveAction = {
                    model.contentProperty().set(editor.htmlText)
                    model.saveToDrafts()
                }
            }
        }

        val sendB = ButtonHelper.button(ActionHelper.create(Icons.send(), sendAction, Strings.SEND, false))
        val attachB = SplitMenuButton()
        attachB.setOnAction {
            attachFile(stage)?.forEach { f ->
                val attachment = OutgoingAttachment(f.toPath())
                model.attachments.add(OutgoingAttachment(f.toPath()))
                try
                {
                    attachB.items.add(0, Attachments.addRemoveMenu(attachment, model.attachments, attachB.items))
                }
                catch (ex: IOException)
                {
                    Logger.getLogger(ComposeMessage::class.java.name).log(Level.SEVERE, null, ex)
                }
                catch (ex: StorageException)
                {
                    Logger.getLogger(ComposeMessage::class.java.name).log(Level.SEVERE, null, ex)
                }
            }
        }
        attachB.graphic = Icons.attach()
        try
        {
            Attachments.addRemoveMenu(model.attachments, attachB.items)
        }
        catch (ex: IOException)
        {
            Logger.getLogger(ComposeMessage::class.java.name).log(Level.SEVERE, null, Dimension.LinearUnits.ex)
        }
        catch (ex: StorageException)
        {
            Logger.getLogger(ComposeMessage::class.java.name).log(Level.SEVERE, null, Dimension.LinearUnits.ex)
        }

        val saveB = ButtonHelper.button(ActionHelper.create(Icons.save(), saveAction, Strings.SAVE_TO_DRAFTS, false))

        toolBar.spacing = 1.0
        toolBar.children.addAll(sendB, attachB, saveB)

        root.spacing = 5.0
        root.padding = INSETS
        stage.scene = scene

        stage.show()
    }
}
