package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.IntegerProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import org.knowtiphy.pinkpigmail.ITreeItem
import org.knowtiphy.owlorm.javafx.IPeer

/**
 * @author graham
 */
interface IFolder : ITreeItem, IPeer
{
    val account: IAccount

    val messages: ObservableList<IMessage>

    val isTrash: Boolean

    val isInbox: Boolean

    val nameProperty: StringProperty

    val messageCountProperty: IntegerProperty

    val unreadMessageCountProperty: IntegerProperty

    fun deleteMessages(messages: Collection<IMessage>)

    fun markMessagesAsRead(messages: Collection<IMessage>)

    fun markMessagesAsJunk(messages: Collection<IMessage>)

    fun markMessagesAsNotJunk(messages: Collection<IMessage>)

    //fun loadAhead(messages: Collection<IMessage>): Future<*>
}