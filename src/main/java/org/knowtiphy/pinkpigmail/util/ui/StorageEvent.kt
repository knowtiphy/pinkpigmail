package org.knowtiphy.pinkpigmail.util.ui

import org.apache.jena.rdf.model.Model

class StorageEvent(val eid : String, val type: String, val aid : String, val model : Model)
{
	override fun toString(): String
	{
		return "StorageEvent(eid='$eid', type='$type', aid='$aid', model=$model)"
	}
}