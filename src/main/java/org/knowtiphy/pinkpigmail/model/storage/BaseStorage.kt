package org.knowtiphy.pinkpigmail.model.storage

import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.Model
import org.knowtiphy.babbage.storage.IOldStorageListener
import org.knowtiphy.babbage.storage.IReadContext
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.IStorageListener
import java.util.concurrent.Future

open class BaseStorage(val storage: IStorage) : AutoCloseable
{
	override fun close() = storage.close()

	fun sync(aid: String): Model = storage.sync(aid)

	fun sync(aid: String, fid: String): Future<*> = storage.sync(aid, fid)

	fun doOperation(operation: Model): Future<*> = storage.doOperation(operation);

	fun addListener(listener: IStorageListener) = storage.addListener(listener)

	fun query(id: String, query: String): ResultSet = storage.query(id, query)

	//	TODO -- at a guess everything below here needs to go -- maybe not the read context stuff

	val readContext: IReadContext get() = storage.readContext

	fun addListener(listener: IOldStorageListener): Map<String, Future<*>> = storage.addOldListener(listener)
}