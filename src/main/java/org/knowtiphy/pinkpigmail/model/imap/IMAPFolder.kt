package org.knowtiphy.pinkpigmail.model.imap

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import org.apache.jena.arq.querybuilder.SelectBuilder
import org.apache.jena.graph.NodeFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.sparql.core.Var
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.model.storage.MailStorage
import org.knowtiphy.pinkpigmail.util.ui.StorageEvent
import org.knowtiphy.utils.JenaUtils
import org.knowtiphy.utils.JenaUtils.P
import org.knowtiphy.utils.JenaUtils.R
import org.knowtiphy.utils.NameSource

/**
 * @author graham
 */
class IMAPFolder(folderId: String, override val account: IEmailAccount, storage: MailStorage) :
	StoredPeer<MailStorage>(folderId, storage), IFolder
{
	companion object
	{
		val FOLDER_ATTRIBUTES: SelectBuilder = SelectBuilder()
			.addVar("*")
			.addWhere("?id", "?p", "?o")
			.addFilter("?p != <${Vocabulary.CONTAINS}>")

		val MESSAGE_IDS_IN_FOLDER: SelectBuilder = SelectBuilder()
			.addVar("*")
			.addWhere("?id", "<${Vocabulary.CONTAINS}>", "?mid")
	}

	override val messages: ObservableList<IMessage> = FXCollections.observableArrayList()

	//	this just replicates PEERS but maybe they are going away
	//	TODO -- this whole map thing is a hacky mess
	private val messageMap: ObservableMap<String, IMAPMessage> = FXCollections.observableHashMap()

	override val nameProperty = SimpleStringProperty()
	override val messageCountProperty = SimpleIntegerProperty()
	override val unreadMessageCountProperty = SimpleIntegerProperty()

	override val isArchiveProperty = SimpleBooleanProperty(false)
	override val isDraftsProperty = SimpleBooleanProperty(false)
	override val isInboxProperty = SimpleBooleanProperty(false)
	override val isJunkProperty = SimpleBooleanProperty(false)
	override val isTrashProperty = SimpleBooleanProperty(false)
	override val isSentProperty = SimpleBooleanProperty(false)

	private val eventHandlers = HashMap<String, (StorageEvent) -> Unit>()

	init
	{
		declareU(Vocabulary.HAS_MESSAGE_COUNT, messageCountProperty)
		declareU(Vocabulary.HAS_UNREAD_MESSAGE_COUNT, unreadMessageCountProperty)
		declareU(Vocabulary.HAS_NAME, nameProperty)

		eventHandlers[Vocabulary.FOLDER_SYNCED] = ::folderSyncHandler
		eventHandlers[Vocabulary.MESSAGE_FLAGS_CHANGED] = ::flagsChangedHandler
		eventHandlers[Vocabulary.MESSAGE_ARRIVED] = ::addMessageHandler
		eventHandlers[Vocabulary.MESSAGE_DELETED] = ::deleteMessageHandler
	}

	fun initialize()
	{
		val uriID = NodeFactory.createURI(id)

		//	initialize the data properties of this folder
		FOLDER_ATTRIBUTES.setVar(Var.alloc("id"), uriID)
		initialize(storage.query(account.id, FOLDER_ATTRIBUTES.buildString()))

		//	initialize the messages in this folder
		MESSAGE_IDS_IN_FOLDER.setVar(Var.alloc("id"), uriID)
		storage.query(account.id, MESSAGE_IDS_IN_FOLDER.buildString()).forEach {
			addMessage(IMAPMessage(it.get("mid").toString(), this, storage))
		}
	}

	fun handleEvent(event: StorageEvent)
	{
		eventHandlers[event.type]?.invoke(event)
	}

	override fun markMessagesAsRead(targets: Collection<IMessage>)
	{
		val (eid, model) = getOp(Vocabulary.MARK_READ)
		addHasMessage(eid, model, targets)
		JenaUtils.addDP(model, eid, Vocabulary.HAS_FLAG, true)
		storage.doOperation(model)
	}

	override fun markMessagesAsJunk(targets: Collection<IMessage>)
	{
		val ids = ids(targets)
		if (isJunkProperty.get() || !account.isMoveJunkMessagesToJunk)
		{
			storage.markMessagesAsJunk(account.id, id, ids, true)
		} else
		{
			disable(targets)
			(account as IMAPAccount).getSpecial(Vocabulary.JUNK_FOLDER).id.let { storage.moveMessagesToJunk(account.id, id, ids, it, true) }
		}
	}

	override fun markMessagesAsNotJunk(targets: Collection<IMessage>)
	{
		storage.markMessagesAsJunk(account.id, id, ids(targets), false)
	}

	override fun deleteMessages(targets: Collection<IMessage>)
	{
		disable(targets)
		if (isTrashProperty.get() || !account.isMoveDeletedMessagesToTrash)
		{
			val (eid, model) = getOp(Vocabulary.DELETE_MESSAGE)
			addHasMessage(eid, model, targets)
			storage.doOperation(model)
		} else
		{
			val ids = ids(targets)
			(account as IMAPAccount).getSpecial(Vocabulary.TRASH_FOLDER).id.let { storage.copyMessages(account.id, id, ids, it, true) }
		}
	}

	override fun loadAhead(targets: Collection<IMessage>)
	{
		//  TODO -- need to save these futures somehow -- maybe not since they will get
		//	recomputed only once when a message content is fetched the first time
		storage.loadAhead(account.id, id, ids(targets))
	}

	override fun toString(): String
	{
		return "IMAPFolder(nameProperty=" + nameProperty.get() + ")"
	}

	//	private methods

	private fun ids(targets: Collection<IMessage>) = targets.map { it.id }

	private fun addMessage(message: IMAPMessage)
	{
		assert(!messageMap.containsKey(message.id))
		message.initialize()
		messageMap[message.id] = message
		messages.add(message)
	}

	private fun deleteMessage(mid: String)
	{
		assert(messageMap.containsKey(mid))
		val message = messageMap.remove(mid)
		messages.remove(message)
	}

	//	update the data properties of this folder
	private fun update()
	{
		FOLDER_ATTRIBUTES.setVar(Var.alloc("id"), NodeFactory.createURI(id))
		initialize(storage.query(account.id, FOLDER_ATTRIBUTES.buildString()))
	}

	private fun addHasMessage(eid: String, model: Model, targets: Collection<IMessage>)
	{
		ids(targets).forEach { JenaUtils.addOP(model, eid, Vocabulary.HAS_MESSAGE, it) }
	}

	//	event handling code

	private fun applyOver(event: StorageEvent, property: String, f: (RDFNode) -> Unit)
	{
		event.model.listObjectsOfProperty(
			R(event.model, event.eid), P(event.model, property)
		).forEach {
			f.invoke(it)
		}
	}

	private fun flagsChangedHandler(event: StorageEvent)
	{
		applyOver(event, Vocabulary.HAS_MESSAGE) { messageMap[it.toString()]?.initialize() }
		update()
	}

	private fun addMessageHandler(event: StorageEvent)
	{
		applyOver(event, Vocabulary.HAS_MESSAGE) { addMessage(IMAPMessage(it.toString(), this, storage)) }
		update()
	}

	private fun deleteMessageHandler(event: StorageEvent)
	{
		println("IN DELETE MESSAGE HANDLER")
		applyOver(event, Vocabulary.HAS_MESSAGE) { deleteMessage(it.toString()) }
		update()
	}

	@Suppress("UNUSED_PARAMETER")
	private fun folderSyncHandler(event: StorageEvent)
	{
		println("folderSyncHandler")
		update()
		//	initialize the messages in this folder

		MESSAGE_IDS_IN_FOLDER.setVar(Var.alloc("id"), NodeFactory.createURI(id))
		val stored = HashSet<String>()
		storage.query(account.id, MESSAGE_IDS_IN_FOLDER.buildString()).forEach { stored.add(it.get("mid").toString()) }

		val toDelete = HashSet<IMessage>()
		messages.forEach {
			if (!stored.contains(it.id))
				toDelete.add(it)
		}

		messages.removeAll(toDelete)

		stored.forEach {
			if (!messageMap.containsKey(it))
				addMessage(IMAPMessage(it, this, storage))
		}
	}

	private fun getOp(type: String): Pair<String, Model>
	{
		val opId = NameSource().get()
		val operation = ModelFactory.createDefaultModel()
		JenaUtils.addType(operation, opId, type)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_ACCOUNT, account.id)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_FOLDER, id)
		return Pair(opId, operation)
	}
}