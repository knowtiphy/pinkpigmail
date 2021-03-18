package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import org.knowtiphy.owlorm.javafx.IPeer

/**
 * @author graham
 */
interface IFolder : IPeer
{
    val account: IEmailAccount

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

    fun deleteMessages(targets: Collection<IMessage>)

    fun markMessagesAsRead(targets: Collection<IMessage>)

    fun markMessagesAsJunk(targets: Collection<IMessage>)

    fun markMessagesAsNotJunk(targets: Collection<IMessage>)

    fun loadAhead(targets : Collection<IMessage>)
}