package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.StringProperty
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.knowtiphy.owlorm.javafx.IPeer
import org.knowtiphy.pinkpigmail.model.events.UIEvent
import org.knowtiphy.pinkpigmail.model.storage.StorageEvent
import org.reactfx.EventSource

interface IAccount : IPeer
{
	fun initialize()
	{
	}

	fun sync()
	{
	}

	fun save(model: Model, name: Resource)

	val fromStorage: EventSource<StorageEvent>

	val events: EventSource<UIEvent>

	val nickNameProperty: StringProperty
}