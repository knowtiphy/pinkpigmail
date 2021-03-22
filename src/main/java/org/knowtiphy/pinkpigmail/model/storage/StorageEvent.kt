package org.knowtiphy.pinkpigmail.model.storage

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.PinkPigMail
import org.knowtiphy.utils.JenaUtils

data class StorageEvent(val eid: String, val type: String, val aid: String?, val model: Model)
{
	constructor(eid: RDFNode, type: RDFNode, aid: RDFNode?, model: Model) :
			this(eid.toString(), type.toString(), aid?.toString(), model)

	override fun toString(): String
	{
		return "StorageEvent(eid='$eid', type='$type', aid='$aid', model=$model)"
	}

	fun isA(t : String) = t == type
}