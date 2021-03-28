package org.knowtiphy.pinkpigmail.model

import org.apache.jena.arq.querybuilder.SelectBuilder
import org.apache.jena.graph.NodeFactory
import org.apache.jena.sparql.core.Var
import org.knowtiphy.babbage.storage.IStorage
import java.util.HashSet

class QueryHelper
{
	companion object
	{
		fun diff(storage : IStorage,
		         query : SelectBuilder,
		         id : String,
		         proj : String,
		         map : Map<String, *>) : Triple<Set<String>, Set<String>, Set<String>>
		{
			val stored = HashSet<String>()

			query.setVar(Var.alloc("id"), NodeFactory.createURI(id))
			//println(query.buildString())
			storage.query(query.buildString()).forEach { stored.add(it.getResource(proj).toString()) }

			val toDelete = HashSet<String>()
			val toAdd = HashSet<String>()
			val toUpdate = HashSet<String>()

			stored.forEach { if (!map.containsKey(it)) toAdd.add(it) else toUpdate.add(it) }
			map.keys.forEach { if (!stored.contains(it)) toDelete.add(it) }
			//stored.forEach { if (!map.containsKey(it)) toAdd.add(it) }

			println("STORED = " + stored)
			println("toDelete = " + toDelete)
			println("toAdd = " + toAdd)
			println("toUpdate = " + toUpdate)

			return Triple(toAdd, toDelete, toUpdate)
		}
	}
}