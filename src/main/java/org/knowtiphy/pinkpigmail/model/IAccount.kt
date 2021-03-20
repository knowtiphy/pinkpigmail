package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.StringProperty
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.knowtiphy.owlorm.javafx.IPeer
import org.knowtiphy.pinkpigmail.util.ui.StorageEvent
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

	val events: EventSource<StorageEvent>

	val nickNameProperty: StringProperty
}