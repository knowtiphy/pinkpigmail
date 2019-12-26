package org.knowtiphy.pinkpigmail

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import org.knowtiphy.pinkpigmail.model.EmailModelType
import org.knowtiphy.pinkpigmail.model.IAttachment
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.Operation.Companion.perform
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

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

	fun composeMail(mailAccount: IEmailAccount) = perform {
		ComposeMessage.compose(mailAccount.getSendModel(EmailModelType.COMPOSE)) { it.send() }
	}

	fun markMessagesAsJunk(messages: Collection<IMessage>)
	{
		messages.first().folder.markMessagesAsJunk(messages)
	}

	fun markMessagesAsNotJunk(messages: Collection<IMessage>)
	{
		messages.first().folder.markMessagesAsNotJunk(messages)
	}

	fun deleteMessages(messages: Collection<IMessage>)
	{
		val folder = messages.first().folder
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

	fun replyToMessage(message: IMessage, all: Boolean = false) = perform {
		ComposeMessage.compose(message.mailAccount.getReplyModel(message,
				if (all) EmailModelType.REPLY_ALL else EmailModelType.REPLY)) { it.send() }
	}

	fun forwardMail(message: IMessage) = perform {
		ComposeMessage.compose(message.mailAccount.getReplyModel(message, EmailModelType.FORWARD)) { it.send() }
	}

	fun saveAttachment(attachment: IAttachment)
	{
		val chooser = FileChooser()
		chooser.initialFileName = attachment.fileName
		val f = chooser.showSaveDialog(null)
		if (f != null)
		{
			perform { Files.copy(attachment.inputStream, f.toPath(), StandardCopyOption.REPLACE_EXISTING) }
		}
	}

	fun saveAttachments(attachments: List<IAttachment>)
	{
		val chooser = DirectoryChooser()
		val f = chooser.showDialog(null)
		if (f != null)
		{
			perform {
				attachments.forEach {
					Files.copy(it.inputStream, Paths.get(f.absolutePath, it.fileName), StandardCopyOption.REPLACE_EXISTING)
				}
			}
		}
	}

	fun openAttachment(attachment: IAttachment) = perform { attachment.open() }
}