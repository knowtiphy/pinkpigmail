package org.knowtiphy.pinkpigmail.model

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.Globals
import org.knowtiphy.pinkpigmail.PinkPigMail
import org.knowtiphy.utils.JenaUtils

open class EmailAccount(accountId : String, type : String, storage : IStorage) : BaseAccount(accountId, type, storage)
{
	open fun trustSender(addresses : Collection<EmailAddress>)
	{
		val (eid, model) = getOp(Vocabulary.TRUST_SENDER)
		addresses.forEach {
			println(it.address)
			JenaUtils.addDP(model, eid, Vocabulary.HAS_TRUSTED_SENDER, it.address)
		}
		storage.doOperation(model)
	}

	open fun unTrustSender(addresses : Collection<EmailAddress>)
	{
	}

	open fun trustProvider(url : String)
	{
		val (eid, model) = getOp(Vocabulary.TRUST_SENDER)
		JenaUtils.addOP(model, eid, Vocabulary.HAS_TRUSTED_CONTENT_PROVIDER, url)
		storage.doOperation(model)
	}

	open fun unTrustProvider(url : String)
	{
	}

	private fun getOp(type : String) : Pair<String, Model>
	{
		val opId = Globals.nameSource.get()
		val operation = ModelFactory.createDefaultModel()
		JenaUtils.addType(operation, opId, type)
		JenaUtils.addOP(operation, opId, Vocabulary.HAS_ACCOUNT, accountId)
		return Pair(opId, operation)
	}
}