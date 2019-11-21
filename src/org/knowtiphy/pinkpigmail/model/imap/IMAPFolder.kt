package org.knowtiphy.pinkpigmail.model.imap

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.media.AudioClip
import javafx.scene.media.MediaException
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.pinkpigmail.Fail
import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.owlorm.javafx.Peer
import org.knowtiphy.pinkpigmail.model.PPPeer
import org.knowtiphy.pinkpigmail.resources.Resources
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary

/**
 * @author graham
 */
class IMAPFolder(folderId: String, storage: IStorage) : PPPeer(folderId, storage), IFolder
{
    var imapAccount: IMAPAccount? = null

    override val account: IAccount by lazy {
        imapAccount!!
    }

    override val messages: ObservableList<IMessage> = FXCollections.observableArrayList()
    override val nameProperty = SimpleStringProperty()
    override val messageCountProperty = SimpleIntegerProperty()
    override val unreadMessageCountProperty = SimpleIntegerProperty()

    private val isJunk: Boolean by lazy { Patterns.JUNK_PATTERN.matcher(nameProperty.get()).matches() }
    override val isTrash: Boolean by lazy { Patterns.TRASH_PATTERN.matcher(nameProperty.get()).matches() }
    override val isInbox: Boolean by lazy { Patterns.INBOX_PATTERN.matcher(nameProperty.get()).matches() }

    init
    {
        declareU(Vocabulary.HAS_MESSAGE_COUNT, messageCountProperty)
        declareU(Vocabulary.HAS_UNREAD_MESSAGE_COUNT, unreadMessageCountProperty)
        declareU(Vocabulary.HAS_NAME, nameProperty)
        declareU(Vocabulary.CONTAINS, ::addMessage)
        declareD(Vocabulary.CONTAINS, ::deleteMessage)
    }

    override fun markMessagesAsRead(messages: Collection<IMessage>)
    {
        storage.markMessagesAsRead(account.id, id, ids(messages), true)
    }

    override fun markMessagesAsJunk(messages: Collection<IMessage>)
    {
        val ids = ids(messages)
        if (isJunk || !account.isMoveJunkMessagesToJunk)
        {
            storage.markMessagesAsJunk(account.id, id, ids, true)
        } else
        {
            disable(messages)
            storage.moveMessagesToJunk(account.id, id, ids, imapAccount?.junkFolder?.id, true)
        }
    }

    override fun markMessagesAsNotJunk(messages: Collection<IMessage>)
    {
        storage.markMessagesAsJunk(account.id, id, ids(messages), false)
    }

    override fun deleteMessages(messages: Collection<IMessage>)
    {
        disable(messages)
        val ids = ids(messages)
        if (isTrash || !account.isMoveDeletedMessagesToTrash)
        {
            storage.deleteMessages(account.id, id, ids)
        } else
        {
            storage.copyMessages(account.id, id, ids, imapAccount?.trashFolder?.id, true)
        }
    }

//    override fun loadAhead(messages: Collection<IMessage>): Future<*>
//    {
//        val future = storage.ensureMessageContentLoaded(account.id, id, ids(messages))
//        messages.forEach { it.setFuture(future) }
//        return future
//    }

    private fun ids(msgList: Collection<IMessage>): List<String>
    {
        return msgList.map { x -> x.id }
    }

    private fun deleteMessage(stmt: Statement)
    {
        messages.remove(PEERS[stmt.getObject().toString()])
    }

    private fun addMessage(stmt: Statement)
    {
        val message = PEERS[stmt.getObject().toString()] as IMAPMessage
        message.folder = this
        messages.add(message)
        if (Patterns.INBOX_PATTERN.matcher(nameProperty.get()).matches() && !message.readProperty.get())
        {
            try
            {
                _beep?.play()
            } catch (ex: Exception)
            {
                Fail.failNoMessage(ex)
            }
        }
    }

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as IMAPFolder

        if (nameProperty != other.nameProperty) return false

        return true
    }

    override fun hashCode(): Int
    {
        var result = super.hashCode()
        result = 31 * result + nameProperty.hashCode()
        return result
    }

    private val _beep: AudioClip? =
            try
            {
                AudioClip(Resources::class.java.getResource("beep-29.wav").toString())
            } catch (ex: MediaException)
            {
                Fail.failNoMessage(ex)
                null
            }
}