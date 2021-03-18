package org.knowtiphy.pinkpigmail.model.storage

import org.apache.jena.rdf.model.Model
import org.knowtiphy.babbage.storage.IMAP.MessageModel
import org.knowtiphy.babbage.storage.IStorage
import java.util.concurrent.Future

//	a mail storage facade on storage
//	eventually will replace this with methods using RDF models

class MailStorage(storage: IStorage) : BaseStorage(storage)
{
	val specialFolders: Model get() = storage.specialFolders

	fun ensureMessageContentLoaded(accountId: String, folderId: String, messageId: String): Future<*> =
		storage.ensureMessageContentLoaded(accountId, folderId, messageId)

	fun loadAhead(accountId: String, folderId: String, messageIds: Collection<String>): Future<*> =
		storage.loadAhead(accountId, folderId, messageIds)

	fun send(model: MessageModel) = storage.send(model)

	//  TODO -- mark vs expunge
	fun moveMessagesToJunk(
		accountId: String, sourceFolderId: String, messageIds: Collection<String>,
		targetFolderId: String, delete: Boolean
	): Future<*> =
		storage.moveMessagesToJunk(accountId, sourceFolderId, messageIds, targetFolderId, delete)

	fun copyMessages(
		accountId: String, sourceFolderId: String, messageIds: Collection<String>,
		targetFolderId: String, delete: Boolean
	): Future<*> = storage.copyMessages(accountId, sourceFolderId, messageIds, targetFolderId, delete)

	fun markMessagesAsAnswered(
		accountId: String, folderId: String, messageIds: Collection<String>, flag: Boolean
	): Future<*> = storage.markMessagesAsAnswered(accountId, folderId, messageIds, flag)

	fun markMessagesAsJunk(
		accountId: String, folderId: String, messageIds: Collection<String>, flag: Boolean
	): Future<*> = storage.markMessagesAsJunk(accountId, folderId, messageIds, flag)
}