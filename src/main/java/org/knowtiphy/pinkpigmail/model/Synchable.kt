package org.knowtiphy.pinkpigmail.model

import org.apache.jena.rdf.model.ModelFactory
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.Globals
import org.knowtiphy.utils.JenaUtils
import java.util.concurrent.Future

open class Synchable(id : String, type : String, storage : IStorage) : StoredPeer(id, type, storage)
{
	open fun sync() : Future<*>
	{
		val opId = Globals.nameSource.get()
		val operation = ModelFactory.createDefaultModel()
		JenaUtils.addType(operation, opId, Vocabulary.SYNC)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_RESOURCE, uri)
		return storage.doOperation(operation)
	}
}