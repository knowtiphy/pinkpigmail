package org.knowtiphy.pinkpigmail.model

import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.model.events.UIEvent
import org.knowtiphy.pinkpigmail.model.storage.StorageEvent
import org.reactfx.EventSource

open class BaseAccount<T>(val accountId: String, storage: T) : StoredPeer<T>(accountId, storage)
{
	val fromStorage = EventSource<StorageEvent>()

	val eventHandlers = HashMap<String, (StorageEvent) -> Unit>()

	init
	{
		fromStorage.subscribe(::handleEvent)
	}

	val events = EventSource<UIEvent>()

	private fun handleEvent(event: StorageEvent)
	{
		if (eventHandlers.containsKey(event.type))
			eventHandlers[event.type]?.invoke(event)
	}
}