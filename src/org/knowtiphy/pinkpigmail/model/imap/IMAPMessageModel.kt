package org.knowtiphy.pinkpigmail.model.imap

import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.model.MessageModel
import org.knowtiphy.pinkpigmail.model.SendMode
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.IMAP.Mime
import org.knowtiphy.babbage.storage.StorageException
import java.io.IOException

/*
 * @author graham
 */
class IMAPMessageModel(storage: IStorage, account: IMAPAccount, copyTo: IMAPFolder,
                       replyToMessage: IMessage?, sendMode: SendMode,
                       subject: String?, to: String?, content: String?) : MessageModel(storage, account,
        replyToMessage, sendMode, subject, to, content, copyTo)
{
    @Throws(StorageException::class, IOException::class)
    override fun send()
    {
        val account = mailAccount
        //  TODO -- need to check addresses
        val model = org.knowtiphy.babbage.storage.IMAP.MessageModel(account.id,
                subjectProperty().get(), toProperty().get(), ccProperty().get(),
                contentProperty().get(), if (sendMode === SendMode.TEXT) Mime.PLAIN else Mime.HTML,
                attachments.map { it.location }, copyTo.id)
        //  TODO -- this assumes that the send works -- need to chain them together
        storage.send(model)//account.sentFolder.getId());
        if (replyToMessage != null)
        {
            storage.markMessagesAsAnswered(account.id,
                    replyToMessage.folder.id, listOf(replyToMessage.id), true)
        }
    }

    @Throws(StorageException::class)
    override fun saveToDrafts()
    {
        save()
        //val account = account as IMAPAccount
        //getStorage().saveToDrafts(account.getId(), account.draftsFolder.getId());
    }
}