package org.knowtiphy.pinkpigmail.model.imap

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.PeerState
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.pinkpigmail.model.IMessage

/**
 * @author graham
 */
class IMAPFolder(folderId: String, storage: IStorage) : StoredPeer(folderId, storage), IFolder
{
    override val accountProperty = SimpleObjectProperty<IEmailAccount>()

    //  TODO -- why do we have two of these? dumb hack to get around type system with null checking -- change this
    private val mailAccount: IMAPAccount by lazy {
        accountProperty.get()!! as IMAPAccount
    }

    override val messages: ObservableList<IMessage> = FXCollections.observableArrayList()
    override val nameProperty = SimpleStringProperty()
    override val messageCountProperty = SimpleIntegerProperty()
    override val unreadMessageCountProperty = SimpleIntegerProperty()
    override val isArchiveProperty = SimpleBooleanProperty()
    override val isDraftsProperty = SimpleBooleanProperty()
    override val isInboxProperty = SimpleBooleanProperty()
    override val isJunkProperty = SimpleBooleanProperty()
    override val isTrashProperty = SimpleBooleanProperty()
    override val isSentProperty = SimpleBooleanProperty()

    init
    {
        declareU(Vocabulary.HAS_MESSAGE_COUNT, messageCountProperty)
        declareU(Vocabulary.HAS_UNREAD_MESSAGE_COUNT, unreadMessageCountProperty)
        declareU(Vocabulary.HAS_NAME, nameProperty)
        declareU(Vocabulary.IS_ARCHIVE_FOLDER, isArchiveProperty)
        declareU(Vocabulary.IS_DRAFTS_FOLDER, isDraftsProperty)
        declareU(Vocabulary.IS_INBOX, isInboxProperty)
        declareU(Vocabulary.IS_JUNK_FOLDER, isJunkProperty)
        declareU(Vocabulary.IS_SENT_FOLDER, isSentProperty)
        declareU(Vocabulary.IS_TRASH_FOLDER, isTrashProperty)
        declareU(Vocabulary.CONTAINS, ::addMessage)
        declareD(Vocabulary.CONTAINS, messages)
    }

    override fun markMessagesAsRead(messages: Collection<IMessage>)
    {
        storage.markMessagesAsRead(mailAccount.id, id, ids(messages), true)
    }

    override fun markMessagesAsJunk(messages: Collection<IMessage>)
    {
        val ids = ids(messages)
        if (isJunkProperty.get() || !mailAccount.isMoveJunkMessagesToJunk)
        {
            storage.markMessagesAsJunk(mailAccount.id, id, ids, true)
        } else
        {
            disable(messages)
            storage.moveMessagesToJunk(mailAccount.id, id, ids, mailAccount.junkFolder?.id, true)
        }
    }

    override fun markMessagesAsNotJunk(messages: Collection<IMessage>)
    {
        storage.markMessagesAsJunk(mailAccount.id, id, ids(messages), false)
    }

    override fun deleteMessages(messages: Collection<IMessage>)
    {
        disable(messages)
        val ids = ids(messages)
        if (isTrashProperty.get() || !mailAccount.isMoveDeletedMessagesToTrash)
        {
            storage.deleteMessages(mailAccount.id, id, ids)
        } else
        {
            storage.copyMessages(mailAccount.id, id, ids, mailAccount.trashFolder?.id, true)
        }
    }
    
    private fun ids(msgList: Collection<IMessage>): List<String>
    {
        return msgList.map { x -> x.id }
    }

    private fun addMessage(stmt: Statement)
    {
        val message = PeerState.peer(stmt.getObject().asResource())!! as IMAPMessage
        assert(!messages.contains(message)) { message.id }

        message.folder = this
        messages.add(message)
    }

    override fun loadAhead(messages: Collection<IMessage>)
    {
        storage.loadAhead(mailAccount.id, id, ids(messages))
    }

    override fun toString(): String
    {
        return "IMAPFolder(nameProperty=" + nameProperty.get() + ")"
    }
}