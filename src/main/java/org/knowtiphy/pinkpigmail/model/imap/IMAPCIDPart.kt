package org.knowtiphy.pinkpigmail.model.imap

import org.apache.jena.query.ParameterizedSparqlString
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.Entity
import org.knowtiphy.utils.JenaUtils
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author graham
 */
class IMAPCIDPart(id : String, val storage : IStorage) : Entity(id, Vocabulary.IMAP_MESSAGE_CID_PART),
	IHasMimeType by HasMimeType()
{
	companion object
	{
		private val GET_MIME_TYPE = ParameterizedSparqlString(
			"select * where { ?s <" + Vocabulary.HAS_MIME_TYPE + "> ?mimeType. }"
		)

		private val GET_CONTENT = ParameterizedSparqlString(
			"select * where { ?s <" + Vocabulary.HAS_CONTENT + "> ?content. }"
		)
	}

	private var mt : String? = null

	//  TODO -- why dont we know this when we make the CIDPart?
	override var mimeType : String? = null
		get()
		{
			if (mt == null)
			{
				GET_MIME_TYPE.setIri("s", id)
				val resultSet = storage.query(GET_MIME_TYPE.toString())
				mt = JenaUtils.single(resultSet) { JenaUtils.getS(it, "mimeType") }
			}

			//  TODO -- why do I need the !! here? Didn't in IMAPMessage
			return mt!!
		}

	val inputStream : InputStream
		get()
		{
			println("CALLONG CID IStream")
			GET_CONTENT.setIri("s", id)
			val resultSet = storage.query(GET_CONTENT.toString())
			return ByteArrayInputStream(JenaUtils.single(resultSet) { JenaUtils.getBA(it, "content") })
		}
}