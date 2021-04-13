package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.IntegerProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import org.knowtiphy.owlorm.javafx.IEntity
import java.util.concurrent.Future

/**
 * @author graham
 */
interface IFolder : IEntity
{
	val account : IEmailAccount

	val nameProperty : StringProperty

	val messageCountProperty : IntegerProperty

	val unreadMessageCountProperty : IntegerProperty

	val messages : ObservableList<IMessage>

	fun isSpecial(type : String) : Boolean

	fun deleteMessages(targets : Collection<IMessage>)

	fun markMessagesAsRead(targets : Collection<IMessage>)

	fun markMessagesAsJunk(targets : Collection<IMessage>)

	fun markMessagesAsNotJunk(targets : Collection<IMessage>)

	fun markMessagesAsAnswered(targets : Collection<IMessage>)

	fun sync() : Future<*>

	fun syncAhead(indices : List<Int>, targets : Collection<IMessage>)
}