package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.StringProperty
import org.knowtiphy.owlorm.javafx.IEntity
import org.knowtiphy.pinkpigmail.model.events.UIEvent
import org.knowtiphy.pinkpigmail.model.storage.StorageEvent
import org.reactfx.EventSource
import java.util.concurrent.Future

interface IAccount : IEntity
{
	fun initialize();

	fun sync() : Future<*>;

	val fromStorage: EventSource<StorageEvent>

	val events: EventSource<UIEvent>

	val nickNameProperty : StringProperty
}