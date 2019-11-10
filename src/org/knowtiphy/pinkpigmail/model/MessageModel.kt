package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.StorageException

/**
 * @author graham
 */
abstract class MessageModel(val storage: IStorage, override val account: IAccount,
                            val replyToMessage: IMessage?, override val sendMode: SendMode,
                            subject: String?, to: String?, content: String?, override val copyTo: IFolder) : IMessageModel
{
    private val subjectProperty: StringProperty
    private val toProperty: StringProperty
    private val ccProperty = SimpleStringProperty()
    private val contentProperty: StringProperty
    override val attachments: ObservableList<IAttachment> = FXCollections.observableArrayList()

    init
    {
        subjectProperty = SimpleStringProperty(subject)
        toProperty = SimpleStringProperty(to)
        contentProperty = SimpleStringProperty(content)
    }

    override fun contentProperty(): StringProperty
    {
        return contentProperty
    }

    override fun subjectProperty(): StringProperty
    {
        return subjectProperty
    }

    override fun toProperty(): StringProperty
    {
        return toProperty
    }

    override fun ccProperty(): StringProperty
    {
        return ccProperty
    }

    override fun addAttachment(attachment: IAttachment)
    {
        attachments.add(attachment)
        attachments.sort()
    }

    @Throws(StorageException::class)
    override fun save()
    {
        //        System.err.println("Save");
        //        IWriteContext context = storage.getWriteContext();
        //        context.startTransaction();
        //        Model model = context.getModel();
        //        Resource res = model.createResource(messageId);
        //        try
        //        {
        //            model.remove(model.listStatements(res, model.createProperty(Vocabulary.HAS_SUBJECT), (RDFNode) null));
        //            model.remove(model.listStatements(res, model.createProperty(Vocabulary.TO), (RDFNode) null));
        ////            model.remove(model.listStatements(res, model.createProperty(Vocabulary.HAS_CC), (RDFNode) null));
        ////            model.remove(model.listStatements(res, model.createProperty(Vocabulary.HAS_BCC), (RDFNode) null));
        //            model.remove(model.listStatements(res, model.createProperty(Vocabulary.HAS_CONTENT), (RDFNode) null));
        //            //  this is wrong -- need to split the address lists up
        //            Store.attr(model, messageId, Vocabulary.HAS_SUBJECT, subjectProperty.get());
        //            try
        //            {
        //                new InternetAddress(toProperty.get());
        //                Store.attr(model, messageId, Vocabulary.TO, toProperty.get());
        //            }
        //            catch (NullPointerException | AddressException ex)
        //            {
        //                System.out.println("TO IS WONKY");
        //            }
        //            //Store.attr(model, messageId, Vocabulary.HAS_CC, ccProperty.getAttr());
        //            Store.attr(model, messageId, Vocabulary.HAS_CONTENT, contentProperty.get());
        //            context.commit();
        //        }
        //        catch (Exception ex)
        //        {
        //            ex.printStackTrace();
        //            context.abort();
        //        }
        //        finally
        //        {
        //            context.endTransaction();
        //        }
    }
}