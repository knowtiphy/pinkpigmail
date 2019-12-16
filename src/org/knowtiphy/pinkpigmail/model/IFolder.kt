package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import org.knowtiphy.owlorm.javafx.IPeer
import org.knowtiphy.pinkpigmail.ITreeItem

/**
 * @author graham
 */
interface IFolder : ITreeItem, IPeer
{
    val accountProperty: ObjectProperty<IEmailAccount>

    val messages: ObservableList<IMessage>

    val isArchiveProperty: BooleanProperty
    val isDraftsProperty: BooleanProperty
    val isInboxProperty: BooleanProperty
    val isJunkProperty: BooleanProperty
    val isSentProperty: BooleanProperty
    val isTrashProperty: BooleanProperty

    val nameProperty: StringProperty

    val messageCountProperty: IntegerProperty

    val unreadMessageCountProperty: IntegerProperty

    fun deleteMessages(messages: Collection<IMessage>)

    fun markMessagesAsRead(messages: Collection<IMessage>)

    fun markMessagesAsJunk(messages: Collection<IMessage>)

    fun markMessagesAsNotJunk(messages: Collection<IMessage>)
}