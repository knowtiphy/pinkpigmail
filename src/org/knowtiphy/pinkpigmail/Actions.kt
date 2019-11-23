package org.knowtiphy.pinkpigmail

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IAttachment
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.model.ModelType
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.babbage.storage.StorageException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.logging.Level
import java.util.logging.Logger

/**
 *
 * @author graham
 */
object Actions
{
    @Throws(ExecutionException::class, InterruptedException::class)
    @JvmStatic
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

    @Throws(ExecutionException::class, InterruptedException::class)
    @JvmStatic
    fun composeMail(mailAccount: IEmailAccount)
    {
        try
        {
            val model = mailAccount.getSendModel(ModelType.COMPOSE)
            ComposeMessage.compose(model) { Operation.perform { model.send() } }
        } catch (ex: StorageException)
        {
            Fail.fail(ex)
        }
    }

    @JvmStatic
    fun markMessagesAsJunk(messages: List<IMessage>)
    {
        messages[0].folder.markMessagesAsJunk(messages)
    }

    @JvmStatic
    fun markMessagesAsNotJunk(messages: List<IMessage>)
    {
        messages[0].folder.markMessagesAsNotJunk(messages)
    }

    @JvmStatic
    fun deleteMessages(messages: List<IMessage>)
    {
        val folder = messages[0].folder
        if (folder.isTrash)
        {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.width = 500.0
            alert.height = 700.0
            alert.title = Strings.DELETE
            alert.contentText = Strings.DELETE_FOREVER
            alert.dialogPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE)
            if (alert.showAndWait().isPresent && alert.result != ButtonType.OK)
            {
                return
            }
        }

        //  make a copy since the msgList is the table currentMessageChanged and deleting changes the currentMessageChanged
        folder.deleteMessages(ArrayList(messages))
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    @JvmStatic
    fun replyToMessage(message: IMessage, all: Boolean)
    {
        try
        {
            val model = message.mailAccount.getReplyModel(message, if (all) ModelType.REPLY_ALL else ModelType.REPLY)
            ComposeMessage.compose(model) { Operation.perform { model.send() } }
        } catch (ex: StorageException)
        {
            Fail.fail(ex)
        }
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    @JvmStatic
    fun forwardMail(message: IMessage)
    {
        try
        {
            val model = message.mailAccount.getReplyModel(message, ModelType.FORWARD)
            ComposeMessage.compose(model) { Operation.perform { model.send() } }
        } catch (ex: StorageException)
        {
            Fail.fail(ex)
        }
    }

    @JvmStatic
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

    @JvmStatic
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

    @JvmStatic
    fun openAttachment(attachment: IAttachment)
    {
        try
        {
            attachment.open()
        } catch (ex: IOException)
        {
            Fail.fail(ex)
        } catch (ex: StorageException)
        {
            Fail.fail(ex)
        }
    }
}
