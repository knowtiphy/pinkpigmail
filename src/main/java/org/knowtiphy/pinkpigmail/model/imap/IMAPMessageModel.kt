package org.knowtiphy.pinkpigmail.model.imap

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.babbage.storage.exceptions.StorageException
import org.knowtiphy.pinkpigmail.Globals
import org.knowtiphy.pinkpigmail.model.EmailSendMode
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.model.MessageModel
import org.knowtiphy.pinkpigmail.util.Mime
import org.knowtiphy.utils.JenaUtils
import java.io.IOException
import java.util.*

/*
 * @author graham
 */
class IMAPMessageModel(
	storage : IStorage,
	account : IMAPAccount,
	ccs : IMAPFolder,
	replyTo : IMessage?,
	sendMode : EmailSendMode,
	subject : String?,
	tos : String?,
	content : String?
) : MessageModel(storage, account, replyTo, sendMode, subject, tos, content, ccs)
{
	@Throws(StorageException::class, IOException::class)
	override fun send()
	{
		val account = account
		//  TODO -- this assumes that the send works -- need to chain them together
		//  just include in the model
		storage.doOperation(createOperation())
		replyToMessage?.folder?.markMessagesAsAnswered(listOf(replyToMessage))
	}

	@Throws(StorageException::class)
	override fun saveToDrafts()
	{
		save()
		//val account = account as IMAPAccount
		//getStorage().saveToDrafts(account.getId(), account.draftsFolder.getId());
	}

	private fun createOperation() : Model
	{
		val mid = Globals.nameSource.get()
		val opId = Globals.nameSource.get()

		val operation = ModelFactory.createDefaultModel()

		JenaUtils.addType(operation, opId, Vocabulary.SEND_MESSAGE)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_ACCOUNT, account.id)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_MESSAGE, mid)

		JenaUtils.addDPN(operation, mid, Vocabulary.HAS_SUBJECT, subjectProperty().get())
		toList(toProperty().get()).forEach { JenaUtils.addDP(operation, mid, Vocabulary.TO, it) }
		toList(ccProperty().get()).forEach { JenaUtils.addDP(operation, mid, Vocabulary.HAS_CC, it) }
		JenaUtils.addDPN(operation, mid, Vocabulary.HAS_CONTENT, contentProperty().get())
		JenaUtils.addDP(
			operation, mid, Vocabulary.HAS_MIME_TYPE, if (sendMode === EmailSendMode.TEXT) Mime.PLAIN else Mime.HTML
		)

		//  TODO -- attachments, cids
		//  TODO -- need to check addresses
//            attachments.map { it.location }, copyTo.id)

		return operation
	}

	private fun toList(raw : String?) : List<String>
	{
		if (raw == null)
		{
			return LinkedList()
		}

		val trim = raw.trim()
		return if (trim.isEmpty()) LinkedList() else trim.split(",").map { it.trim() }
	}
}