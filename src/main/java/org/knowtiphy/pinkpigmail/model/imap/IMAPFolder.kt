package org.knowtiphy.pinkpigmail.model.imap

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
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.Globals
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.model.Synchable
import org.knowtiphy.pinkpigmail.model.events.FinishSyncEvent
import org.knowtiphy.pinkpigmail.model.events.MessageArrivedEvent
import org.knowtiphy.pinkpigmail.model.events.StartSyncEvent
import org.knowtiphy.pinkpigmail.model.storage.StorageEvent
import org.knowtiphy.utils.JenaUtils
import org.knowtiphy.utils.JenaUtils.P
import org.knowtiphy.utils.JenaUtils.R
import java.util.*
import java.util.concurrent.Future
import kotlin.collections.ArrayList

/**
 * @author graham
 */
class IMAPFolder(folderId : String, override val account : IMAPAccount, storage : IStorage) :
	Synchable(folderId, Vocabulary.IMAP_FOLDER, storage), IFolder
{
	companion object
	{
		val MESSAGE_IDS_IN_FOLDER : SelectBuilder =
			SelectBuilder().addVar("*").addWhere("?id", "<${Vocabulary.CONTAINS}>", "?mid")
	}

	override val messages : ObservableList<IMessage> = FXCollections.observableArrayList()

	private val messageMap : ObservableMap<String, IMAPMessage> = FXCollections.observableHashMap()

	override val nameProperty = SimpleStringProperty()
	override val messageCountProperty = SimpleIntegerProperty()
	override val unreadMessageCountProperty = SimpleIntegerProperty()

	private val eventHandlers = HashMap<String, (StorageEvent) -> Unit>()

	init
	{
		addUpdater(Vocabulary.HAS_MESSAGE_COUNT, messageCountProperty)
		addUpdater(Vocabulary.HAS_UNREAD_MESSAGE_COUNT, unreadMessageCountProperty)
		addUpdater(Vocabulary.HAS_NAME, nameProperty)

		eventHandlers[Vocabulary.FOLDER_SYNCED] = ::folderSyncHandler
		eventHandlers[Vocabulary.MESSAGE_FLAGS_CHANGED] = ::flagsChangedHandler
		eventHandlers[Vocabulary.MESSAGE_ARRIVED] = ::addMessageHandler
		eventHandlers[Vocabulary.MESSAGE_DELETED] = ::deleteMessageHandler
	}

	fun initialize()
	{
		val uriID = NodeFactory.createURI(uri)

		//	initialize the data properties of this folder
		initialize(attributes)

		//	initialize the messages in this folder
		MESSAGE_IDS_IN_FOLDER.setVar(Var.alloc("id"), uriID)
		storage.query(MESSAGE_IDS_IN_FOLDER.buildString()).forEach {
			addMessage(IMAPMessage(it.get("mid").toString(), this, storage))
		}
	}

	override fun sync() : Future<*>
	{
		Globals.push(StartSyncEvent(this, account))
		return super.sync()
	}

	override fun isSpecial(type : String) : Boolean
	{
		return account.getSpecial(type) == this
	}

	fun handleEvent(event : StorageEvent)
	{
		eventHandlers[event.type]?.invoke(event)
	}

	private fun markMessages(targets : Collection<IMessage>, op : String, flag : Boolean?)
	{
		val (eid, model) = getOp(op)
		ids(targets).forEach { JenaUtils.addOP(model, eid, Vocabulary.HAS_MESSAGE, it) }
		if (flag != null)
		{
			JenaUtils.addDP(model, eid, Vocabulary.HAS_FLAG, true)
		}
		storage.doOperation(model)
	}

	override fun markMessagesAsRead(targets : Collection<IMessage>)
	{
		markMessages(targets, Vocabulary.MARK_READ, true)
	}

	//  warning - you may have needed to copy targets in case targets is actually part of a selection model
	//  which can change before calling this message
	//  I dont think you should have to do that unless you are doing a later {} call here
	override fun markMessagesAsJunk(targets : Collection<IMessage>)
	{
		if (isSpecial(Vocabulary.JUNK_FOLDER) || !account.isMoveJunkMessagesToJunk)
		{
			markMessages(targets, Vocabulary.MARK_JUNK, true)
		} else
		{
			//disable(targets)
			//	TODO -- huh?
			account.getSpecial(Vocabulary.JUNK_FOLDER).uri.let {
				storage.moveMessagesToJunk(account.uri, uri, ids(targets), it, true)
			}
		}
	}

	override fun markMessagesAsNotJunk(targets : Collection<IMessage>)
	{
		markMessages(targets, Vocabulary.MARK_JUNK, false)
	}

	override fun markMessagesAsAnswered(targets : Collection<IMessage>)
	{
		markMessages(targets, Vocabulary.MARK_ANSWERED, true)
	}

	override fun deleteMessages(targets : Collection<IMessage>)
	{
		//disable(targets)
		println("deleteMessages -- START")
		println(targets)
		if (isSpecial(Vocabulary.JUNK_FOLDER) || !account.isMoveDeletedMessagesToTrash)
		{
			markMessages(targets, Vocabulary.DELETE_MESSAGE, null)
		} else
		{
			println("deleteMessages --- SPECIAL CASE")
			println(targets)
			val trash = account.getSpecial(Vocabulary.TRASH_FOLDER)
			storage.copyMessages(account.uri, uri, ids(targets), trash.uri, true)
		}
	}

//  NOT REALLY SURE THIS FUNCTION SHOULD BE HERE
//  allows each folder to choose a loadAhead strategy;
//  load ahead radially 3 messages either side of the selection
//	this needs to be improved for when we have multiple selections
//  Note: selection model indices are on the sorted list, not the folder.messages

	override fun syncAhead(indices : List<Int>, targets : Collection<IMessage>)
	{
		//println("syncAhead targets " + targets)

		//  load ahead radially 3 messages either side of the selection
		//	this needs to be improved for when we have multiple selections
		//  Note: selection model indices are on the sorted list, not the folder.messages

		val pos = indices.first()
		val targetsA = targets.toTypedArray()
		val result = ArrayList<ArrayList<IMessage>>()
		val range = targets.indices
		for (i in 1 until 4)
		{
			val disti = ArrayList<IMessage>()
			val after = pos + i
			if (after in range)
			{
				disti.add(targetsA[after])
			}
			val before = pos - i
			if (before in range)
			{
				disti.add(targetsA[before])
			}

			result.add(disti)
		}

		//println(result)
		result.forEach {
			//  TODO -- need to save these futures somehow -- maybe not since they will get
			//	recomputed only once when a message content is fetched the first time
			val (oid, operation) = getOp(Vocabulary.SYNC_AHEAD)
			ids(it).forEach { mid -> JenaUtils.addOP(operation, oid, Vocabulary.HAS_MESSAGE, mid) }
			storage.doOperation(operation)
		}
	}

	override fun toString() : String
	{
		return "IMAPFolder(nameProperty=" + nameProperty.get() + ")"
	}

//	private methods

	private fun ids(targets : Collection<IMessage>) = targets.map { it.uri }

	private fun addMessage(message : IMAPMessage)
	{
		assert(!messageMap.containsKey(message.uri))
		message.initialize()
		messageMap[message.uri] = message
		messages.add(message)
	}

	private fun deleteMessage(mid : String)
	{
		assert(messageMap.containsKey(mid))
		val message = messageMap.remove(mid)
		messages.remove(message)
	}

	//	update the data properties of this folder
	private fun update()
	{
		initialize(attributes)
	}

//	event handling code

	private fun <T> map(event : StorageEvent, property : String, f : (RDFNode) -> T) : Collection<T>
	{
		val result = LinkedList<T>()
		event.model.listObjectsOfProperty(R(event.model, event.eid), P(event.model, property)).forEach {
			result.add(f.invoke(it))
		}
		return result
	}

	private fun flagsChangedHandler(event : StorageEvent)
	{
		map(event, Vocabulary.HAS_MESSAGE) { messageMap[it.toString()]?.initialize() }
		update()
	}

	private fun addMessageHandler(event : StorageEvent)
	{
		val newMessages = map(event, Vocabulary.HAS_MESSAGE) { IMAPMessage(it.toString(), this, storage) }
		newMessages.forEach { addMessage(it) }
		update()
		Globals.push(MessageArrivedEvent(account, this))
	}

	private fun deleteMessageHandler(event : StorageEvent)
	{
		println("IN DELETE MESSAGE HANDLER")
		//  note: there may be no deleted messages in the event since in some cases the server may
		//  inadvertently batch deletes
		map(event, Vocabulary.HAS_MESSAGE) { println(it); deleteMessage(it.toString()) }
		update()
	}

	@Suppress("UNUSED_PARAMETER")
	private fun folderSyncHandler(event : StorageEvent)
	{
		update()
		//	initialize the messages in this folder

		MESSAGE_IDS_IN_FOLDER.setVar(Var.alloc("id"), NodeFactory.createURI(uri))
		val stored = HashSet<String>()
		storage.query(MESSAGE_IDS_IN_FOLDER.buildString()).forEach { stored.add(it.get("mid").toString()) }

		val toDelete = HashSet<IMessage>()
		messages.forEach {
			if (!stored.contains(it.uri)) toDelete.add(it)
		}

		messages.removeAll(toDelete)

		stored.forEach {
			if (!messageMap.containsKey(it)) addMessage(IMAPMessage(it, this, storage))
		}

		Globals.push(FinishSyncEvent(this,account))
	}

	private fun getOp(type : String) : Pair<String, Model>
	{
		val opId = Globals.nameSource.get()
		val operation = ModelFactory.createDefaultModel()
		JenaUtils.addType(operation, opId, type)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_ACCOUNT, account.uri)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_FOLDER, uri)
		return Pair(opId, operation)
	}
}