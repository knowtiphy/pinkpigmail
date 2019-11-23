package org.knowtiphy.pinkpigmail.model.caldav

import com.calendarfx.model.CalendarSource
import javafx.beans.property.SimpleStringProperty
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.ICalendarAccount
import org.knowtiphy.pinkpigmail.model.PPPeer

/**
 * @author graham
 */
class CalDAVAccount(accountId: String, storage: IStorage) : PPPeer(accountId, storage), ICalendarAccount
{
    override val source = CalendarSource()
    override val emailAddressProperty = SimpleStringProperty()

    val serverNameProperty = SimpleStringProperty()
    val serverHeaderProperty = SimpleStringProperty()
    val passwordProperty = SimpleStringProperty()

    init
    {
        declareU(Vocabulary.HAS_SERVER_NAME, serverNameProperty)
        declareU(Vocabulary.HAS_SERVER_HEADER, serverHeaderProperty)
        declareU(Vocabulary.HAS_EMAIL_ADDRESS, emailAddressProperty)
        declareU(Vocabulary.HAS_PASSWORD, passwordProperty)
        declareU(Vocabulary.CONTAINS, ::addCalendar)
    }

    override fun save(model: Model, name: Resource)
    {
        model.add(name, model.createProperty(Vocabulary.RDF_TYPE), model.createResource(Vocabulary.CALDAV_ACCOUNT))
        model.add(name, model.createProperty(Vocabulary.HAS_SERVER_NAME), serverNameProperty.get())
        model.add(name, model.createProperty(Vocabulary.HAS_SERVER_HEADER), serverHeaderProperty.get())
        model.add(name, model.createProperty(Vocabulary.HAS_EMAIL_ADDRESS), emailAddressProperty.get())
        model.add(name, model.createProperty(Vocabulary.HAS_PASSWORD), passwordProperty.get())
    }

    private fun addCalendar(stmt: Statement)
    {
        val calendar = PEERS[stmt.getObject().toString()] as CalDAVCalendar
        //  TODO -- should possibly do something better here
        if (!calendar.calendar.name.equals("Outbox") && !calendar.calendar.name.equals("Inbox"))
        {
            source.calendars.add(calendar.calendar)
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
