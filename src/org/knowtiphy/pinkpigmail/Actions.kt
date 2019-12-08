package org.knowtiphy.pinkpigmail

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import org.knowtiphy.babbage.storage.StorageException
import org.knowtiphy.pinkpigmail.model.IAttachment
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.model.ModelType
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.Operation
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 *
 * @author graham
 */
object Actions
{
    fun configureAccount(mailAccount: IEmailAccount)
    {
//        val root = VBox()
//
//        with(root)
//        {
//            form {
//                fieldset("Personal Info") {
//                    field("First Name") {
//                        textfield()
//                    }
//                    field("Last Name") {
//                        textfield()
//                    }
//                    field("Birthday") {
//                        datepicker()
//                    }
//                }
//                fieldset("Contact") {
//                    field("Phone") {
//                        textfield()
//                    }
//                    field("Email") {
//                        textfield()
//                    }
//                }
//                button("Commit") {
//                    action { println("Wrote to database!") }
//                }
//            }
//        }
//
//        val stage = Stage()
//        stage.scene = Scene(root)
//        stage.showAndWait()
    }

    fun composeMail(mailAccount: IEmailAccount) = Operation.perform {
        val model = mailAccount.getSendModel(ModelType.COMPOSE)
        ComposeMessage.compose(model) { Operation.perform { model.send() } }
    }

    fun markMessagesAsJunk(messages: List<IMessage>)
    {
        messages[0].folder.markMessagesAsJunk(messages)
    }

    fun markMessagesAsNotJunk(messages: List<IMessage>)
    {
        messages[0].folder.markMessagesAsNotJunk(messages)
    }

    fun deleteMessages(messages: List<IMessage>)
    {
        val folder = messages[0].folder
        if (folder.isTrashProperty.get())
        {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            with(alert) {
                width = 500.0
                height = 700.0
                title = Strings.DELETE
                contentText = Strings.DELETE_FOREVER
                dialogPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE)
            }
            if (alert.showAndWait().isPresent && alert.result != ButtonType.OK)
            {
                return
            }
        }

        //  make a copy since the msgList is the table currentMessageChanged and deleting changes the currentMessageChanged
        folder.deleteMessages(ArrayList(messages))
    }

    fun replyToMessage(message: IMessage, all: Boolean) = Operation.perform {
        val model = message.mailAccount.getReplyModel(message, if (all) ModelType.REPLY_ALL else ModelType.REPLY)
        ComposeMessage.compose(model) { Operation.perform { model.send() } }
    }

    fun forwardMail(message: IMessage) = Operation.perform {
        val model = message.mailAccount.getReplyModel(message, ModelType.FORWARD)
        ComposeMessage.compose(model) { model.send() }
    }

    fun saveAttachment(attachment: IAttachment)
    {
        val chooser = FileChooser()
        chooser.initialFileName = attachment.fileName
        val f = chooser.showSaveDialog(null)
        if (f != null)
        {
            try
            {
                Files.copy(attachment.inputStream, f.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (ex: IOException)
            {
                Logger.getLogger(Attachments::class.java.name).log(Level.SEVERE, null, ex)
            } catch (ex: StorageException)
            {
                Logger.getLogger(Attachments::class.java.name).log(Level.SEVERE, null, ex)
            }
        }
    }

    fun saveAttachments(attachments: List<IAttachment>)
    {
        val chooser = DirectoryChooser()
        val f = chooser.showDialog(null)
        if (f != null)
        {
            try
            {
                for (attachment in attachments)
                {
                    Files.copy(attachment.inputStream, Paths.get(f.absolutePath, attachment.fileName), StandardCopyOption.REPLACE_EXISTING)
                }
            } catch (ex: IOException)
            {
                Logger.getLogger(Attachments::class.java.name).log(Level.SEVERE, null, ex)
            } catch (ex: StorageException)
            {
                Logger.getLogger(Attachments::class.java.name).log(Level.SEVERE, null, ex)
            }
        }
    }

    fun openAttachment(attachment: IAttachment) = Operation.perform { attachment.open() }
}