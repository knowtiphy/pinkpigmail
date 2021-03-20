package org.knowtiphy.pinkpigmail.util.ui

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode

data class StorageEvent(val eid : String, val type: String, val aid : String?, val model : Model)
{
	constructor( eid : RDFNode,  type: RDFNode,  aid : RDFNode?,  model : Model) :
			this(eid.toString(), type.toString(), aid?.toString(), model)

	override fun toString(): String
	{
		return "StorageEvent(eid='$eid', type='$type', aid='$aid', model=$model)"
	}
}