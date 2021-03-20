package org.knowtiphy.pinkpigmail.model

import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.util.ui.StorageEvent
import org.reactfx.EventSource

public open class BaseAccount<T>(accountId: String, storage: T) : StoredPeer<T>(accountId, storage)
{
	val events =  EventSource<StorageEvent>()
}