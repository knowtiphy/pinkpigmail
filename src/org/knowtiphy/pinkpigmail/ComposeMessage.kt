package org.knowtiphy.pinkpigmail

import javafx.event.ActionEvent
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.SplitMenuButton
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.web.HTMLEditor
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.knowtiphy.pinkpigmail.model.IMessageModel
import org.knowtiphy.pinkpigmail.model.SendMode
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.babbage.storage.StorageException
import org.knowtiphy.pinkpigmail.util.ActionHelper
import org.knowtiphy.pinkpigmail.util.ButtonHelper
import org.knowtiphy.pinkpigmail.util.UIUtils
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

    fun compose(model: IMessageModel, send: () -> Unit)
    {
        val stage = UIUtils.getStage(800.0, 700.0)
        stage.title = Strings.FROM + " : " + model.account.emailAddressProperty.get()

        val toolBar = HBox()
        val root = VBox(header(model), toolBar)

        var sendAction: ((ActionEvent) -> Unit)? = null
        var saveAction: ((ActionEvent) -> Unit)? = null

        when (model.sendMode)
        {
            SendMode.TEXT ->
            {
                val editor = TextArea()
                editor.text = model.contentProperty().get()
                model.contentProperty().bind(editor.textProperty())
                editor.positionCaret(1)
                VBox.setVgrow(editor, Priority.ALWAYS)
                root.children.add(editor)
                sendAction = {
                    stage.close()
                    send.invoke()
                }
                saveAction = {
                    try
                    {
                        model.saveToDrafts()
                    } catch (ex: StorageException)
                    {
                        Logger.getLogger(ComposeMessage::class.java.name).log(Level.SEVERE, null, ex)
                    }
                }
            }
            SendMode.HTML ->
            {
                val editor = HTMLEditor()
                editor.htmlText = model.contentProperty().get()
                //				editor.setOnKeyPressed(evt ->
                //				{
                //					System.out.println("XXX:" + evt.getText());
                //					if (evt.getCode() == KeyCode.ENTER)
                //					{
                //						System.err.println(evt);
                //						//editor.fireEvent(new KeyEvent());
                //						KeyEvent ke = new KeyEvent(evt.getSource(), evt.getTarget(), evt.getEventType(),
                //								"M", "M", KeyCode.M, false, false, false, false);
                //						System.err.println("ENTER");
                //						System.err.println(ke);
                //						editor.fireEvent(ke);
                //						editor.setHtmlText(editor.getHtmlText()+ "</p><p></p>");
                //					}
                //				});
                //	TODO -- initialize the insert cursor at the beginning?
                VBox.setVgrow(editor, Priority.ALWAYS)
                root.children.add(editor)
                sendAction = {
                    stage.close()
                    model.contentProperty().set(editor.htmlText)
                    send.invoke()
                }
                saveAction = {
                    model.contentProperty().set(editor.htmlText)
                    model.saveToDrafts()
                }
            }
        }

        val sendB = ButtonHelper
                .regular(ActionHelper.create(Icons.send(Icons.MEDIUM_SIZE), sendAction, Strings.SEND, false))
        val attachB = SplitMenuButton()
        attachB.setOnAction {
            val files = attachFile(stage)
            if (files != null)
            {
                for (f in files)
                {
                    val attachment = OutgoingAttachment(f.toPath())
                    model.attachments.add(OutgoingAttachment(f.toPath()))
                    try
                    {
                        attachB.items.add(0, Attachments.addRemoveMenu(attachment, model.attachments, attachB.items))
                    } catch (ex: IOException)
                    {
                        Logger.getLogger(ComposeMessage::class.java.name).log(Level.SEVERE, null, ex)
                    } catch (ex: StorageException)
                    {
                        Logger.getLogger(ComposeMessage::class.java.name).log(Level.SEVERE, null, ex)
                    }
                }
            }
        }
        attachB.graphic = Icons.attach(Icons.MEDIUM_SIZE)
        try
        {
            Attachments.addRemoveMenu(model.attachments, attachB.items)
        } catch (ex: IOException)
        {
            Logger.getLogger(ComposeMessage::class.java.name).log(Level.SEVERE, null, ex)
        } catch (ex: StorageException)
        {
            Logger.getLogger(ComposeMessage::class.java.name).log(Level.SEVERE, null, ex)
        }

        val saveB = ButtonHelper.regular(ActionHelper.create(Icons.save(Icons.MEDIUM_SIZE), saveAction, Strings.SAVE_TO_DRAFTS, false))

        toolBar.spacing = 1.0
        toolBar.children.addAll(sendB, attachB, saveB)

        root.spacing = 5.0
        root.padding = INSETS
        stage.scene = Scene(root)

        stage.show()
    }
}