package org.knowtiphy.pinkpigmail.model.imap

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.apache.jena.query.QueryExecutionFactory
import org.knowtiphy.babbage.storage.IMAP.Mime
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.*
import org.knowtiphy.utils.JenaUtils
import java.net.URL
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.Future

/**
 * @author graham
 */
class IMAPMessage(id: String, storage: IStorage) : PPPeer(id, storage), IMessage
{
    override val readProperty = SimpleBooleanProperty()
    override val answeredProperty = SimpleBooleanProperty()
    override val junkProperty = SimpleBooleanProperty()
    override val subjectProperty = SimpleStringProperty()
    override val sentOnProperty = SimpleObjectProperty<ZonedDateTime>()
    override val receivedOnProperty = SimpleObjectProperty<ZonedDateTime>()
    override val from: ObservableList<EmailAddress> = FXCollections.observableArrayList()
    override val to: ObservableList<EmailAddress> = FXCollections.observableArrayList()
    override val cc: ObservableList<EmailAddress> = FXCollections.observableArrayList()
    override val bcc: ObservableList<EmailAddress> = FXCollections.observableArrayList()

    override val loadRemoteProperty = SimpleBooleanProperty(false)

    override lateinit var folder: IFolder
    override val mailAccount by lazy { folder.accountProperty.get() }
    private var future: Future<*>? = null

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

    @Synchronized
    override fun ensureContentLoaded(immediate: Boolean)
    {
        if (future == null)
        {
            //  TODO probably want to do different things in embedded vs non embedded mode (e.g. cache the future)
            future = storage.ensureMessageContentLoaded(folder.accountProperty.get().id, folder.id, id, immediate)
            future!!.get()
        }
    }

    override fun getContent(allowHTML: Boolean): IPart
    {
        ensureContentLoaded(true)
        val context = storage.readContext
        context.start()
        try
        {
            val mimeType = JenaUtils.getS(JenaUtils.listObjectsOfPropertyU(context.model, id, Vocabulary.HAS_MIME_TYPE))
            //  TODO this replace stuff should be done in the database
            val content = JenaUtils.getS(JenaUtils.listObjectsOfPropertyU(context.model,
                    id, Vocabulary.HAS_CONTENT)).replace("\\\"", "\"")
            return IMAPPart(id, storage, mimeType, content)
        } finally
        {
            context.end()
        }
    }

    override val attachments: ObservableList<IAttachment>
        get()
        {
            ensureContentLoaded(true)
            val result = FXCollections.observableArrayList<IAttachment>()
            val context = storage.readContext
            context.start()
            try
            {
                val resultSet = QueryExecutionFactory.create(Fetch.attachments(id), context.model).execSelect()
                resultSet.forEach {
                    result.add(IMAPAttachment(
                            it.get(Fetch.VAR_ATTACHMENT_ID).asResource().toString(),
                            storage,
                            it.get(Fetch.VAR_FILE_NAME).asLiteral().toString(),
                            it.get(Fetch.VAR_MIME_TYPE).asLiteral().toString()))
                }
            } finally
            {
                context.end()
            }

            return result
        }

    override val cidMap: Map<URL, IMAPCIDPart>
        get()
        {
            ensureContentLoaded(true)
            val result = HashMap<URL, IMAPCIDPart>()
            val context = storage.readContext
            context.start()
            try
            {
                val resultSet = QueryExecutionFactory.create(Fetch.cidLocalNames(id), context.model).execSelect()
                resultSet.forEach {
                    result[URL(it.get(Fetch.VAR_LOCAL_CID_PART_ID).asResource().toString())] = IMAPCIDPart(it.get(Fetch.VAR_CID_PART_ID).asResource().toString(), storage)
                }
            } finally
            {
                context.end()
            }

            return result
        }

    override val isHTML: Boolean
        get()
        {
            ensureContentLoaded(true)
            val context = storage.readContext
            context.start()
            try
            {
                return JenaUtils.getS(JenaUtils.listObjectsOfPropertyU(context.model, id,
                        Vocabulary.HAS_MIME_TYPE)) == Mime.HTML
            } finally
            {
                context.end()
            }
        }
}