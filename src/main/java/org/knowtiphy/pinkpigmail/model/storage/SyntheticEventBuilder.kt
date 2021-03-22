package org.knowtiphy.pinkpigmail.model.storage

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.PinkPigMail
import org.knowtiphy.utils.JenaUtils

class SyntheticEventBuilder(val type: String, val aid: String?)
{
	constructor(type: String) : this(type, null)

	private var model: Model = ModelFactory.createDefaultModel()
	private val eid = PinkPigMail.nameSource.get()

	init
	{
		JenaUtils.addType(model, eid, type)
		if (aid != null)
		{
			JenaUtils.addOP(model, eid, Vocabulary.HAS_ACCOUNT, aid)
		}
	}

	fun addOP(p: String, o: String?): SyntheticEventBuilder
	{
		JenaUtils.addOP(model, eid, p, o)
		return this
	}

	fun <T> addDP(p: String, o: T): SyntheticEventBuilder
	{
		JenaUtils.addDP(model, eid, p, o)
		return this
	}

	fun build(): StorageEvent
	{
		return StorageEvent(eid, type, aid, model)
	}

	override fun toString(): String
	{
		return "SyntheticEventBuilder(type='$type', model=$model, eid='$eid')"
	}
}