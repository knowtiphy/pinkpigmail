package org.knowtiphy.pinkpigmail.model.imap

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.apache.jena.query.ParameterizedSparqlString
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.knowtiphy.babbage.storage.IMAP.Mime
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.Globals
import org.knowtiphy.pinkpigmail.model.EmailAddress
import org.knowtiphy.pinkpigmail.model.IAttachment
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.model.IPart
import org.knowtiphy.utils.JenaUtils
import java.net.URL
import java.time.ZonedDateTime
import java.util.concurrent.Future

/**
 * @author graham
 */
class IMAPMessage(id : String, override val folder : IFolder, storage : IStorage) :
	StoredPeer(id, Vocabulary.IMAP_MESSAGE, storage), IMessage, IHasMimeType by HasMimeType()
{
	companion object
	{
		private val GET_CONTENT = ParameterizedSparqlString(
			"select * where { ?s <" + Vocabulary.HAS_MIME_TYPE + "> ?mimeType. ?s <" + Vocabulary.HAS_CONTENT + "> ?content}"
		)

		private val GET_MIME_TYPE = ParameterizedSparqlString(
			"select * where { ?s <" + Vocabulary.HAS_MIME_TYPE + "> ?mimeType. }"
		)
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

	//private var mimeType : String? = null

	init
	{
		declareU(Vocabulary.IS_READ, readProperty)
		declareU(Vocabulary.IS_ANSWERED, answeredProperty)
		declareU(Vocabulary.IS_JUNK, junkProperty)
		declareU(Vocabulary.HAS_SUBJECT, subjectProperty)
		declareU(Vocabulary.RECEIVED_ON, receivedOnProperty)
		declareU(Vocabulary.SENT_ON, sentOnProperty)
		declareU(Vocabulary.FROM, from, Funcs.RDF_NODE_TO_EMAIL_ADDRESS)
		declareU(Vocabulary.TO, to, Funcs.RDF_NODE_TO_EMAIL_ADDRESS)
		declareU(Vocabulary.HAS_CC, cc, Funcs.RDF_NODE_TO_EMAIL_ADDRESS)
		declareU(Vocabulary.HAS_BCC, bcc, Funcs.RDF_NODE_TO_EMAIL_ADDRESS)
	}

	fun initialize()
	{
		initialize(attributes)
	}

	override val account : IEmailAccount get() = folder.account

	//  start a synch of a message to make sure the server has it
	override fun sync() : Future<*>
	{
		return storage.doOperation(getOp(Vocabulary.SYNC).second)
	}

	//  don't call this unless you have previously synched and waited on the future it returns
	override fun getContent(allowHTML : Boolean) : IPart
	{
		//  TODO -- close the in mem result set?
		GET_CONTENT.setIri("s", id)
		val resultSet = storage.query(GET_CONTENT.toString())
		val soln = JenaUtils.single(resultSet) { it }

		//  TODO this replace stuff should be done in the database
		val content = JenaUtils.getS(soln, "content").replace("\\\"", "\"")
		val mimeType = JenaUtils.getS(soln, "mimeType")

		return IMAPPart(id, mimeType, content)
	}

	//  don't call this unless you have previously synched and waited on the future it returns
	override val attachments : ObservableList<IAttachment>
		get()
		{
			val result = FXCollections.observableArrayList<IAttachment>()
//			val context = storage.readContext
//			context.start()
//			try
//			{
//				//	TODO -- doesnt close query context
//				val resultSet = QueryExecutionFactory.create(Fetch.attachments(id), context.model).execSelect()
//				resultSet.forEach {
//					result.add(
//						IMAPAttachment(
//							it.get(Fetch.VAR_ATTACHMENT_ID).asResource().toString(),
//							storage,
//							it.get(Fetch.VAR_FILE_NAME).asLiteral().toString(),
//							it.get(Fetch.VAR_MIME_TYPE).asLiteral().toString()
//						)
//					)
//				}
//			} finally
//			{
//				context.end()
//			}

			return result
		}

	//  don't call this unless you have previously synched and waited on the future it returns
	override val cidMap : Map<URL, IMAPCIDPart>
		get()
		{
			val result = HashMap<URL, IMAPCIDPart>()
//			val context = storage.readContext
//			context.start()
//			try
//			{
//				//	TODO -- doesnt close query context .
//				val resultSet = QueryExecutionFactory.create(Fetch.cidLocalNames(id), context.model).execSelect()
//				//  the local CID is a string not a URI -- it is unique within a message, but not across messages
//				resultSet.forEach {
//					result[URL(it.get(Fetch.VAR_LOCAL_CID_PART_ID).toString())] =
//						IMAPCIDPart(it.get(Fetch.VAR_CID_PART_ID).asResource().toString(), storage)
//				}
//			} finally
//			{
//				context.end()
//			}

			return result
		}

	//  don't call this unless you have previously synched and waited on the future it returns
	override val isHTML : Boolean
		get()
		{
			if (mimeType == null)
			{
				GET_MIME_TYPE.setIri("s", id)
				val resultSet = storage.query(GET_MIME_TYPE.toString())
				mimeType = JenaUtils.single(resultSet) { JenaUtils.getS(it, "mimeType") }
			}

			return mimeType == Mime.HTML
		}

	private fun getOp(type : String) : Pair<String, Model>
	{
		val opId = Globals.nameSource.get()
		val operation = ModelFactory.createDefaultModel()
		JenaUtils.addType(operation, opId, type)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_ACCOUNT, account.id)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_FOLDER, folder.id)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_MESSAGE, id)
		return Pair(opId, operation)
	}
}