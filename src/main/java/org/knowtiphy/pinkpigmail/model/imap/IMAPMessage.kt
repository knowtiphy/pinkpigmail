package org.knowtiphy.pinkpigmail.model.imap

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.apache.jena.arq.querybuilder.SelectBuilder
import org.apache.jena.graph.NodeFactory
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.sparql.core.Var
import org.knowtiphy.babbage.storage.IMAP.Mime
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.PinkPigMail
import org.knowtiphy.pinkpigmail.model.*
import org.knowtiphy.pinkpigmail.model.storage.MailStorage
import org.knowtiphy.utils.JenaUtils
import java.net.URL
import java.time.ZonedDateTime
import java.util.concurrent.Future

/**
 * @author graham
 */
class IMAPMessage(id : String, override val folder : IFolder, storage : MailStorage) :
	StoredPeer<MailStorage>(id, storage), IMessage
{
	companion object
	{
		//val GET_ATTRIBUTES = "SELECT ?p ?o WHERE { <${id}> ?p ?o . }"
		val GET_ATTRIBUTES : SelectBuilder = SelectBuilder().addVar("*").addWhere("?id", "?p", "?o")
	}

	override val readProperty = SimpleBooleanProperty()
	override val answeredProperty = SimpleBooleanProperty()
	override val junkProperty = SimpleBooleanProperty()
	override val subjectProperty = SimpleStringProperty()
	override val sentOnProperty = SimpleObjectProperty<ZonedDateTime>()
	override val receivedOnProperty = SimpleObjectProperty<ZonedDateTime>()
	override val from : ObservableList<EmailAddress> = FXCollections.observableArrayList()
	override val to : ObservableList<EmailAddress> = FXCollections.observableArrayList()
	override val cc : ObservableList<EmailAddress> = FXCollections.observableArrayList()
	override val bcc : ObservableList<EmailAddress> = FXCollections.observableArrayList()

	override val loadRemoteProperty = SimpleBooleanProperty(false)

	init
	{
		declareU(Vocabulary.IS_READ, readProperty)
		declareU(Vocabulary.IS_ANSWERED, answeredProperty)
		declareU(Vocabulary.IS_JUNK, junkProperty)
		declareU(Vocabulary.HAS_SUBJECT, subjectProperty)
		declareU(Vocabulary.RECEIVED_ON, receivedOnProperty)
		declareU(Vocabulary.SENT_ON, sentOnProperty)
		declareU(Vocabulary.FROM, from, Funcs.STMT_TO_EMAIL_ADDRESS)
		declareU(Vocabulary.TO, to, Funcs.STMT_TO_EMAIL_ADDRESS)
		declareU(Vocabulary.HAS_CC, cc, Funcs.STMT_TO_EMAIL_ADDRESS)
		declareU(Vocabulary.HAS_BCC, bcc, Funcs.STMT_TO_EMAIL_ADDRESS)
	}

	fun initialize()
	{
		//	build the data properties of this message
		GET_ATTRIBUTES.setVar(Var.alloc("id"), NodeFactory.createURI(id))
		initialize(storage.query(GET_ATTRIBUTES.buildString()))
	}

	fun update()
	{
		//	build the data properties of this message
		GET_ATTRIBUTES.setVar(Var.alloc("id"), NodeFactory.createURI(id))
		initialize(storage.query(GET_ATTRIBUTES.buildString()))
	}

	override val account : IEmailAccount
		get() = folder.account

	override fun sync() : Future<*>
	{
		return storage.doOperation(getOp(Vocabulary.SYNC).second)
	}

	private fun ensureContentLoaded()
	{
		sync().get()
	}

	override fun getContent(allowHTML : Boolean) : IPart
	{
		ensureContentLoaded()
		val context = storage.readContext
		context.start()
		try
		{
			val mimeType = JenaUtils.getS(context.model, id, Vocabulary.HAS_MIME_TYPE)
			//  TODO this replace stuff should be done in the database
			val content = JenaUtils.getS(context.model, id, Vocabulary.HAS_CONTENT)
				.replace("\\\"", "\"")
			return IMAPPart(id, mimeType, content)
		}
		finally
		{
			context.end()
		}
	}

	override val attachments : ObservableList<IAttachment>
		get()
		{
			ensureContentLoaded()
			val result = FXCollections.observableArrayList<IAttachment>()
			val context = storage.readContext
			context.start()
			try
			{
				val resultSet = QueryExecutionFactory.create(Fetch.attachments(id), context.model).execSelect()
				resultSet.forEach {
					result.add(IMAPAttachment(it.get(Fetch.VAR_ATTACHMENT_ID).asResource().toString(),
						storage,
						it.get(Fetch.VAR_FILE_NAME).asLiteral().toString(),
						it.get(Fetch.VAR_MIME_TYPE).asLiteral().toString()))
				}
			}
			finally
			{
				context.end()
			}

			return result
		}

	override val cidMap : Map<URL, IMAPCIDPart>
		get()
		{
			ensureContentLoaded()
			val result = HashMap<URL, IMAPCIDPart>()
			val context = storage.readContext
			context.start()
			try
			{
				val resultSet = QueryExecutionFactory.create(Fetch.cidLocalNames(id), context.model).execSelect()
				//  the local CID is a string not a URI -- it is unique within a message, but not across messages
				resultSet.forEach {
					result[URL(it.get(Fetch.VAR_LOCAL_CID_PART_ID).toString())] =
						IMAPCIDPart(it.get(Fetch.VAR_CID_PART_ID).asResource().toString(), storage)
				}
			}
			finally
			{
				context.end()
			}

			return result
		}

	override val isHTML : Boolean
		get()
		{
			ensureContentLoaded()
			val context = storage.readContext
			context.start()
			try
			{
				return JenaUtils.getS(context.model, id, Vocabulary.HAS_MIME_TYPE) == Mime.HTML
			}
			finally
			{
				context.end()
			}
		}

	private fun getOp(type : String) : Pair<String, Model>
	{
		val opId = PinkPigMail.nameSource.get()
		val operation = ModelFactory.createDefaultModel()
		JenaUtils.addType(operation, opId, type)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_ACCOUNT, account.id)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_FOLDER, folder.id)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_MESSAGE, id)
		return Pair(opId, operation)
	}
}