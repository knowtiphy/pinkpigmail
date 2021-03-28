package org.knowtiphy.pinkpigmail.model.imap

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import org.apache.jena.arq.querybuilder.SelectBuilder
import org.apache.jena.graph.NodeFactory
import org.apache.jena.sparql.core.Var
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.babbage.storage.exceptions.StorageException
import org.knowtiphy.pinkpigmail.PinkPigMail
import org.knowtiphy.pinkpigmail.model.EmailAccount
import org.knowtiphy.pinkpigmail.model.EmailAddress
import org.knowtiphy.pinkpigmail.model.EmailModelType
import org.knowtiphy.pinkpigmail.model.EmailReplyMode
import org.knowtiphy.pinkpigmail.model.EmailSendMode
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.model.IMessageModel
import org.knowtiphy.pinkpigmail.model.events.FolderSyncStartedEvent
import org.knowtiphy.pinkpigmail.model.storage.MailStorage
import org.knowtiphy.pinkpigmail.model.storage.StorageEvent
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.utils.JenaUtils.P
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.ExecutionException

/**
 * @author graham
 */
class IMAPAccount(accountId: String, storage: MailStorage) : EmailAccount<MailStorage>(accountId, storage), IEmailAccount
{
	companion object
	{
		val GET_ACCOUNT_ATTRIBUTES: SelectBuilder = SelectBuilder()
			.addVar("*")
			.addWhere("?aid", "?p", "?o")

		val GET_FOLDER_IDS: SelectBuilder = SelectBuilder()
			.addVar("*")
			.addWhere("?aid", "<${Vocabulary.CONTAINS}>", "?fid")
			.addWhere("?fid", "<${RDF.type}>", "<${Vocabulary.IMAP_FOLDER}>")

		val GET_SPECIAL_IDS: SelectBuilder = SelectBuilder()
			.addVar("*")
			.addWhere("?aid", "<${Vocabulary.HAS_SPECIAL}>", "?fid")
			.addWhere("?fid", "<${RDF.type}>", "?type")
			.addWhere("?type", "<${RDFS.subClassOf}>", "<${Vocabulary.IMAP_FOLDER}>")
			.addFilter("?type != <${Vocabulary.IMAP_FOLDER}>")
	}

	override val nickNameProperty = SimpleStringProperty()
	override val allowHTMLProperty = SimpleBooleanProperty(true)
	override val folders: ObservableMap<String, IFolder> = FXCollections.observableHashMap()
	override val trustedContentProviders: ObservableList<String> = FXCollections.observableArrayList()
	override val trustedSenders: ObservableList<EmailAddress> = FXCollections.observableArrayList()
	override val serverNameProperty = SimpleStringProperty()
	override val emailAddressProperty = SimpleStringProperty()

	private val password = SimpleStringProperty()
	private val replyMode = EmailReplyMode.MATCH
	private val sendMode = EmailSendMode.TEXT

	private val specials: ObservableMap<String, String> = FXCollections.observableHashMap<String, String>()

	private val setting = AccountSettings()

	init
	{
		declareU(Vocabulary.HAS_SERVER_NAME, serverNameProperty)
		declareU(Vocabulary.HAS_EMAIL_ADDRESS, emailAddressProperty)
		declareU(Vocabulary.HAS_PASSWORD, password)
		declareU(Vocabulary.HAS_NICK_NAME, nickNameProperty)
		declareU(Vocabulary.HAS_TRUSTED_CONTENT_PROVIDER, trustedContentProviders)
		declareU(Vocabulary.HAS_TRUSTED_SENDER, trustedSenders, Funcs.STMT_TO_EMAIL_ADDRESS)

		emailAddressProperty.addListener { _: ObservableValue<out String?>, _: String?, newValue: String? ->
			if (nickNameProperty.get() == null)
				nickNameProperty.set(newValue)
		}

		eventHandlers[Vocabulary.FOLDER_SYNCED] = ::folderBasedEvent
		eventHandlers[Vocabulary.MESSAGE_FLAGS_CHANGED] = ::folderBasedEvent
		eventHandlers[Vocabulary.MESSAGE_ARRIVED] = ::folderBasedEvent
		eventHandlers[Vocabulary.MESSAGE_DELETED] = ::folderBasedEvent
	}

	override fun initialize()
	{
		storage.sync(id)

		val aidR = NodeFactory.createURI(id)

		//	initialize the attributes of this account
		GET_ACCOUNT_ATTRIBUTES.setVar(Var.alloc("aid"), aidR)
		storage.query(GET_ACCOUNT_ATTRIBUTES.buildString()).forEach {
			initialize(it)
		}

		//	work out the special folders
		GET_SPECIAL_IDS.setVar(Var.alloc("aid"), aidR)
		storage.query(GET_SPECIAL_IDS.buildString()).forEach {
			specials[it.get("type").toString()] = it.get("fid").toString()
		}

		//	get the folders in this account
		GET_FOLDER_IDS.setVar(Var.alloc("aid"), aidR)
		storage.query(GET_FOLDER_IDS.buildString()).forEach {
			addFolder(it.getResource("fid").toString())
		}
	}

	override fun sync()
	{
		//	sync all relevant folders -- for the moment just the inbox
		//	for the moment assume the folder structure hasn't changed so just sync the messages
		val inbox = specials[Vocabulary.INBOX_FOLDER]!!
		PinkPigMail.pushEvent(FolderSyncStartedEvent(this, folders[inbox]!!))
		storage.sync(id, inbox)
	}

	fun getSpecial(type: String): IMAPFolder
	{
		return folders[specials[type]!!] as IMAPFolder
	}

	private fun folderBasedEvent(event: StorageEvent)
	{
		event.model.listObjectsOfProperty(P(event.model, Vocabulary.HAS_FOLDER)).forEach {
			(folders[it.toString()] as IMAPFolder).handleEvent(event)
		}
	}

	private fun addFolder(fid: String)
	{
		assert(!folders.contains(fid)) { fid }
		val folder = IMAPFolder(fid, this, storage)
		folder.initialize()
		folders[folder.id] = folder
	}

	override val isMoveDeletedMessagesToTrash: Boolean
		get()
		{
			return setting.isMoveDeletedMessagesToTrash
		}

	override val isMoveJunkMessagesToJunk: Boolean
		get()
		{
			return setting.isMoveJunkMessagesToJunk
		}

	override val isCopySentMessagesToSent: Boolean
		get()
		{
			return setting.isCopySentMessagesToSent
		}

	override val isDisplayMessageMarksAsRead: Boolean
		get()
		{
			return setting.isDisplayMessageMarksAsRead
		}

	override fun trustSender(addresses: Collection<EmailAddress>)
	{
		trustedSenders.addAll(addresses)
		super.trustSender(addresses)
	}

	override fun unTrustSender(addresses: Collection<EmailAddress>)
	{
		trustedSenders.removeAll(addresses)
		super.unTrustSender(addresses)
	}

	override fun isTrustedSender(addresses: Collection<EmailAddress>): Boolean
	{
		return trustedSenders.containsAll(addresses)
	}

	override fun trustProvider(url: String)
	{
		trustedContentProviders.add(url)
		super.trustProvider(url)
	}

	override fun unTrustProvider(url: String)
	{
		trustedContentProviders.remove(url)
		super.unTrustProvider(url)
	}

	override fun isTrustedProvider(url: String): Boolean
	{
		return trustedContentProviders.contains(url)
	}

	override fun getSendModel(modelType: EmailModelType): IMessageModel
	{
		return IMAPMessageModel(
			storage, this, getSpecial(Vocabulary.SENT_FOLDER),
			null, sendMode, null, null, null
		)
	}

	@Throws(StorageException::class, ExecutionException::class, InterruptedException::class)
	override fun getReplyModel(message: IMessage, modelType: EmailModelType): IMessageModel
	{
		val sendMode = sendMode(message, modelType)
		return IMAPMessageModel(
			storage, this, getSpecial(Vocabulary.SENT_FOLDER), message, sendMode,
			(if (modelType == EmailModelType.FORWARD) Strings.FWD else Strings.RE) + message.subjectProperty.get(),
			if (modelType == EmailModelType.FORWARD) null else EmailAddress.format(this, message.from),
			quote(message, sendMode)
		)
	}

	@Throws(StorageException::class)
	private fun quote(message: IMessage, sendMode: EmailSendMode): String
	{
		val builder = StringBuilder()
		if (sendMode == EmailSendMode.HTML)
		{
			builder.append("<html><body>")
			//	TODO -- I don't think is valid HTML for a blank line
			builder.append("<p><br/><br/></p>")
			builder.append("<div style=\"border-left:medium solid #0000ff\">\n")
			builder.append("<div style=\"margin-left: 10px\">\n")
		} else
		{
			builder.append("\n\n> ")
		}
		if (message.sentOnProperty.get() != null)
		{
			builder.append(" ").append(Strings.ON)
			builder.append(message.sentOnProperty.get().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
		}
		val niceFrom = EmailAddress.format(message.account, message.from)
		builder.append(" ").append(niceFrom)
		//        if (!niceFrom.contains("@"))
		//        {
		//            builder.append(" (").append(replyToMessage.getFrom().toString()).append(")");
		//        }
		builder.append(" wrote\n")

		//	TODO -- not sure this is correct
		val raw = message.getContent(sendMode == EmailSendMode.HTML)
		if (sendMode == EmailSendMode.HTML)
		{
			//	TODO: this is a hack
			var processed = raw.content.replace("</html[^>]*>".toRegex(), "</div>")
			processed = processed.replace("</body[^>]*>".toRegex(), "</div>")
			processed = processed.replace("<html[^>]*>".toRegex(), "<div>")
			processed = processed.replace("<body[^>]*>".toRegex(), "<div>")
			builder.append("\n").append(processed)
			builder.append("</div></div></body></html>")
		} else
		{
			builder.append("> \n")
			for (line in raw.content.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
			{
				builder.append("> ").append(line).append("\n")
			}
		}

		return builder.toString()
	}

	@Throws(ExecutionException::class, InterruptedException::class, StorageException::class)
	private fun sendMode(message: IMessage, modelType: EmailModelType): EmailSendMode
	{
		return when (modelType)
		{
			EmailModelType.COMPOSE -> sendMode
			else ->
			{
				if (replyMode == EmailReplyMode.MATCH)
				{
					if (message.isHTML) EmailSendMode.HTML else EmailSendMode.TEXT
				} else
				{
					if (replyMode == EmailReplyMode.HTML) EmailSendMode.HTML else EmailSendMode.TEXT
				}
			}
		}
	}
}

//	private fun deleteFolder(fid: String)
//	{
//		//	TODO -- is this all we need to do?
//		folders.remove(fid)
//	}

//	old way of computing specials -- may go back to it
//		GET_SPECIAL_IDS.setVar(Var.alloc("aid"), NodeFactory.createURI(id))
//		val query = QueryFactory.create(GET_SPECIAL_IDS.buildString())
//		QueryExecutionFactory.create(query, model).execSelect().forEachRemaining {
//			specials[it.get("type").toString()] = it.get("fid").toString()
//		}

//    private void setupOutBox()
//    {
//        String outboxName = E(Vocabulary.OUTBOX, getEmailAddress());
//
//        ReadContext context = getStorage().getReadContext();
//        context.start();
//        try
//        {
//            Model model = context.getModel();
//            StmtIterator it = model.listStatements(model.createResource(getId()),
//                model.createProperty(Vocabulary.CONTAINS), model.createResource(outboxName));
//            if (it.hasNext())
//            {
//                return;
//            }
//        }
//        finally
//        {
//            context.end();
//        }
//
//        WriteContext contextw = getStorage().getWriteContext();
//        contextw.startTransaction();
//        try
//        {
//            Model model = contextw.getModel();
//            Resource accountRes = model.createResource(getId());
//            Resource outBoxRes = model.createResource(outboxName);
//            model.add(accountRes, model.createProperty(Vocabulary.CONTAINS), outBoxRes);
//            model.add(outBoxRes, model.createProperty(RDF.type.toString()), model.createResource(Vocabulary.IMAP_FOLDER));
//            model.add(outBoxRes, model.createProperty(Vocabulary.HAS_NAME), model.createLiteral(Strings.OUTBOX));
//            model.add(outBoxRes, model.createProperty(Vocabulary.HAS_MESSAGE_COUNT), model.createTypedLiteral(0));
//            model.add(outBoxRes, model.createProperty(Vocabulary.HAS_UNREAD_MESSAGE_COUNT), model.createTypedLiteral(0));
//            contextw.commit();
//        }
//        finally
//        {
//            contextw.endTransaction();
//        }
//    }
//
//    @Override
//    public void send(String messageName) throws StorageException
//    {
//        try
//        {
//            MimeMessage message = createMessage();
//            MimeMultipart multipart = new MimeMultipart();
//            MimeBodyPart body = new MimeBodyPart();
//            multipart.addBodyPart(body);
//            message.setContent(multipart);
//
//            String recipients;
//            String ccs;
//            String subject;
//            String content;
//
//            ReadContext context = getStorage().getReadContext();
//            context.start();
//            try
//            {
//                var resultSet = QueryExecutionFactory.create(Queries.fetchOutboxMessage(messageName), context.getModel()).execSelect();
//                QuerySolution s = resultSet.next();
//                recipients = s.getAttr(VAR_TO).asLiteral().getString();
//                ccs = s.getAttr("cc") == null ? null : s.getAttr("cc").asLiteral().getString();
//                subject = s.getAttr(VAR_SUBJECT).asLiteral().getString();
//                content = s.getAttr(VAR_CONTENT).asLiteral().getString();
//            }
//            finally
//            {
//                context.end();
//            }
//
//            System.err.println(recipients);
//            System.err.println(ccs);
//            System.err.println(subject);
//            System.err.println(content);
//
//            message.setFrom(getEmailAddress());
//            message.setReplyTo(new Address[]
//            {
//                getEmailAddress()
//            });
//            for (String to : Format.toList(recipients))
//            {
//                message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
//            }
//            if (ccs != null)
//            {
//                for (String cc : Format.toList(ccs))
//                {
//                    message.addRecipient(Message.RecipientType.CC, new InternetAddress(cc));
//                }
//            }
//            message.setSubject(subject);
//            initializeBody(content, body, multipart, true);
//
//            Transport.send(message);
//            if (setting.isCopySentMessagesToSent())
//            {
//                getStorage().appendMessages(new Message[]
//                {
//                    message
//                }, sentFolder.getId());
//            }
//
//            WriteContext contextw = getStorage().getWriteContext();
//            contextw.startTransaction();
//            try
//            {
//                Queries.unstoreMessage(contextw.getModel(), outbox.getId(), messageName);
//                contextw.commit();
//            }
//            finally
//            {
//                contextw.endTransaction();
//            }
//        }
//        catch (IOException | MessagingException ex)
//        {
//            throw new StorageException(ex);
//        }
//    }

//	private String storeReplyMessage(IMessage message, ModelType modelType, SendMode sendMode) throws StorageException
//	{
//		String replyId = getStorage().getDraftMessageId(this.getId());
//
////        String content = null;
////
////        if (modelType != ModelType.COMPOSE)
////        {
////            assert message != null;
////            content = MessageModel.quote(message, sendMode);
////        }
////
////        IWriteContext context = getStorage().getWriteContext();
////        context.startTransaction();
////        Model model = context.getModel();
////        Resource replyRes = model.createResource(replyId);
////
////        try
////        {
////            DStore.storeOutGoingMessageId(model, getId(), replyId);
////
////            switch (modelType)
////            {
////                case REPLY:
////                case REPLY_ALL:
////                    assert message != null;
////
////                    for (String address : message.getFrom())
////                    {
////                        model.add(replyRes, model.createProperty(Vocabulary.TO), model.createTypedLiteral(address));
////                    }
////                    break;
////                default:
////                    //  do nothing
////            }
////
////            if (modelType != ModelType.COMPOSE)
////            {
////                assert message != null;
////                model.add(replyRes, model.createProperty(Vocabulary.HAS_CONTENT), model.createTypedLiteral(content));
////                String subject = message.subjectProperty().get();
////
////                if (subject != null)
////                {
////                    String header = modelType == ModelType.FORWARD ? Strings.FETCH : Strings.RE;
////                    model.add(replyRes, model.createProperty(Vocabulary.HAS_SUBJECT),
////                            model.createTypedLiteral(subject.startsWith(header) ? subject : header + subject));
////                }
////            }
////
////            model.add(replyRes, model.createProperty(Vocabulary.FROM), model.createTypedLiteral(emailAddress.get()));
////            model.add(replyRes, model.createProperty(Vocabulary.IS_ANSWERED), model.createTypedLiteral(false));
////            model.add(replyRes, model.createProperty(Vocabulary.IS_READ), model.createTypedLiteral(false));
////            model.add(replyRes, model.createProperty(Vocabulary.IS_JUNK), model.createTypedLiteral(false));
////            context.commit();
////
////        } catch (Exception | AssertionError ex)
////        {
////            ex.printStackTrace();
////            context.abort();
////        } finally
////        {
////            context.endTransaction();
////        }
//
//		return replyId;
//	}

//	@Suppress("UNUSED_PARAMETER")
//	private fun syncEvent(event: StorageEvent)
//	{
//		println("ACCOUNT syncEvent " + event)
//		//	add any new folders and delete old ones
//		GET_FOLDER_IDS.setVar(Var.alloc("aid"), NodeFactory.createURI(id))
//		storage.query(id, GET_FOLDER_IDS.buildString()).forEach {
//			val fid = it.get("fid").toString()
//			if (!folders.containsKey(fid))
//			{
//				addFolder(IMAPFolder(fid, this, storage))
//			}
////			else
////			{
////				deleteFolder(fid)
////			}
//		}
//
//		folderBasedEvent(event)
//	}