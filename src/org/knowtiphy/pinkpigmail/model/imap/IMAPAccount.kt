package org.knowtiphy.pinkpigmail.model.imap

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.StorageException
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.*
import org.knowtiphy.pinkpigmail.resources.Strings
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.ExecutionException
import javax.mail.internet.InternetAddress

/**
 * @author graham
 */
class IMAPAccount(accountId: String, storage: IStorage) : PPPeer(accountId, storage), IEmailAccount
{
    override val nickNameProperty = SimpleStringProperty()
    override val allowHTMLProperty = SimpleBooleanProperty(true)
    override val folders: ObservableList<IFolder> = FXCollections.observableArrayList()
    override val trustedContentProviders: ObservableList<String> = FXCollections.observableArrayList()
    override val trustedSenders: ObservableList<EmailAddress> = FXCollections.observableArrayList()
    override val serverNameProperty = SimpleStringProperty()
    override val emailAddressProperty = SimpleStringProperty()

    private val password = SimpleStringProperty()
    private val replyMode = ReplyMode.MATCH
    private val sendMode = SendMode.HTML

    var trashFolder: IMAPFolder? = null
    var junkFolder: IMAPFolder? = null
    private var sentFolder: IMAPFolder? = null
    private var draftsFolder: IMAPFolder? = null

    private val setting = AccountSettings()

    init
    {
        declareU(Vocabulary.HAS_SERVER_NAME, serverNameProperty)
        declareU(Vocabulary.HAS_EMAIL_ADDRESS, emailAddressProperty)
        declareU(Vocabulary.HAS_PASSWORD, password)
        declareU(Vocabulary.HAS_NICK_NAME, nickNameProperty)
        declareU(Vocabulary.HAS_TRUSTED_CONTENT_PROVIDER, trustedContentProviders)
        declareU(Vocabulary.HAS_TRUSTED_SENDER, trustedSenders, Funcs.STMT_TO_EMAIL_ADDRESS)
        declareU(Vocabulary.CONTAINS, ::addFolder)
        declareD(Vocabulary.CONTAINS, folders)
        emailAddressProperty.addListener { _: ObservableValue<out String?>, _: String?, newValue: String? ->
            if (nickNameProperty.get() == null)
                nickNameProperty.set(newValue)
        }
    }

    override fun save(model: Model, name: Resource)
    {
        model.add(name, model.createProperty(Vocabulary.RDF_TYPE), model.createResource(Vocabulary.IMAP_ACCOUNT))
        model.add(name, model.createProperty(Vocabulary.HAS_SERVER_NAME), serverNameProperty.get())
        model.add(name, model.createProperty(Vocabulary.HAS_EMAIL_ADDRESS), emailAddressProperty.get())
        model.add(name, model.createProperty(Vocabulary.HAS_PASSWORD), password.get())
        trustedContentProviders.forEach { model.add(name, model.createProperty(Vocabulary.HAS_TRUSTED_CONTENT_PROVIDER), it) }
        trustedSenders.forEach {
            model.add(name, model.createProperty(Vocabulary.HAS_TRUSTED_SENDER),
                    InternetAddress(it.address, it.personal).toString())
        }
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
    }

    override fun unTrustSender(addresses: Collection<EmailAddress>)
    {
        trustedSenders.removeAll(addresses)
    }

    override fun isTrustedSender(addresses: Collection<EmailAddress>) = trustedSenders.containsAll(addresses)

    override fun trustProvider(url: String)
    {
        trustedContentProviders.add(url)
    }

    override fun unTrustProvider(url: String)
    {
        trustedContentProviders.remove(url)
    }

    override fun isTrustedProvider(url: String) = trustedContentProviders.contains(url)

    override fun getSendModel(modelType: ModelType): IMessageModel
    {
        return IMAPMessageModel(storage, this, sentFolder!!,
                null, sendMode, null, null, null)
    }

    @Throws(StorageException::class, ExecutionException::class, InterruptedException::class)
    override fun getReplyModel(message: IMessage, modelType: ModelType): IMessageModel
    {
        val sendMode = sendMode(message, modelType)
        return IMAPMessageModel(storage, this, sentFolder!!, message, sendMode,
                (if (modelType == ModelType.FORWARD) Strings.FWD else Strings.RE) + message.subjectProperty.get(),
                if (modelType == ModelType.FORWARD) null else EmailAddress.format(this, message.from),
                quote(message, sendMode))
    }

    @Throws(StorageException::class)
    private fun quote(message: IMessage, sendMode: SendMode): String
    {
        val builder = StringBuilder()
        if (sendMode == SendMode.HTML)
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
        val niceFrom = EmailAddress.format(message.mailAccount, message.from)
        builder.append(" ").append(niceFrom)
        //        if (!niceFrom.contains("@"))
        //        {
        //            builder.append(" (").append(replyToMessage.getFrom().toString()).append(")");
        //        }
        builder.append(" wrote\n")

        //	TODO -- not sure this is correct
        val raw = message.getContent(sendMode == SendMode.HTML)
        if (sendMode == SendMode.HTML)
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
    private fun sendMode(message: IMessage, modelType: ModelType): SendMode
    {
        return when (modelType)
        {
            ModelType.COMPOSE -> sendMode
            else ->
            {
                if (replyMode == ReplyMode.MATCH)
                {
                    if (message.isHTML) SendMode.HTML else SendMode.TEXT
                } else
                {
                    if (replyMode == ReplyMode.HTML) SendMode.HTML else SendMode.TEXT
                }
            }
        }
    }

    private fun addFolder(stmt: Statement)
    {
        val folder = peer(stmt.getObject().asResource())!! as IMAPFolder
        assert(!folders.contains(folder)) { folder.id }

        folder.imapAccount = this
        folders.add(folder)

        val name = folder.nameProperty.get() ?: return
        when
        {
            Patterns.TRASH_PATTERN.matcher(name).matches() -> trashFolder = folder
            Patterns.JUNK_PATTERN.matcher(name).matches() -> junkFolder = folder
            Patterns.SENT_PATTERN.matcher(name).matches() -> sentFolder = folder
            Patterns.DRAFTS_PATTERN.matcher(name).matches() -> draftsFolder = folder
        }
    }
}

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
//            model.add(outBoxRes, model.createProperty(Vocabulary.RDF_TYPE), model.createResource(Vocabulary.IMAP_FOLDER));
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
