package org.knowtiphy.pinkpigmail.model.storage

import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.Model
import org.knowtiphy.babbage.storage.IStorageListener
import org.knowtiphy.babbage.storage.IReadContext
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.IOldStorageListener
import java.util.concurrent.Future
import java.util.concurrent.FutureTask

public open class BaseStorage(val storage: IStorage) : AutoCloseable
{
	override fun close() = storage.close()

	fun getAccountInfo(id: String): Model = storage.getAccountInfo(id)

	fun doOperation(operation: Model): Future<*> = storage.doOperation(operation);

	fun addListener(listener: IStorageListener) = storage.addListener(listener)

	//	TODO -- at a guess everything below here needs to go

	fun query(id: String, query: String): ResultSet = storage.query(id, query)

	fun sync(id: String, fid: String) = storage.sync(id, fid)

	val readContext: IReadContext get() = storage.readContext

	fun addListener(listener: IOldStorageListener): Map<String, FutureTask<*>> = storage.addOldListener(listener)
}