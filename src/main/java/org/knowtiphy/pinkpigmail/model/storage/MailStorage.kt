package org.knowtiphy.pinkpigmail.model.storage

import org.knowtiphy.babbage.storage.IMAP.MessageModel
import org.knowtiphy.babbage.storage.IStorage
import java.util.concurrent.Future

//	a mail storage facade on storage
//	eventually will replace this with methods using RDF models so this class will go away

class MailStorage(storage : IStorage) : BaseStorage(storage)
{
	fun send(model : MessageModel) = storage.send(model)

	//  TODO -- mark vs expunge
	fun moveMessagesToJunk(accountId : String,
	                       sourceFolderId : String,
	                       messageIds : Collection<String>,
	                       targetFolderId : String,
	                       delete : Boolean) : Future<*> =
		storage.moveMessagesToJunk(accountId, sourceFolderId, messageIds, targetFolderId, delete)

	fun copyMessages(accountId : String,
	                 sourceFolderId : String,
	                 messageIds : Collection<String>,
	                 targetFolderId : String,
	                 delete : Boolean) : Future<*> =
		storage.copyMessages(accountId, sourceFolderId, messageIds, targetFolderId, delete)

	fun markMessagesAsAnswered(accountId : String,
	                           folderId : String,
	                           messageIds : Collection<String>,
	                           flag : Boolean) : Future<*> =
		storage.markMessagesAsAnswered(accountId, folderId, messageIds, flag)

	fun markMessagesAsJunk(accountId : String,
	                       folderId : String,
	                       messageIds : Collection<String>,
	                       flag : Boolean) : Future<*> =
		storage.markMessagesAsJunk(accountId, folderId, messageIds, flag)
}