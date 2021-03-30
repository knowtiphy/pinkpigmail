package org.knowtiphy.pinkpigmail.model.imap

import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.Entity
import org.knowtiphy.utils.JenaUtils
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author graham
 */
class IMAPCIDPart(id: String, val storage: IStorage) : Entity(id, Vocabulary.IMAP_MESSAGE_CID_PART)
{
    val mimeType: String
        get()
        {
            val context = storage.readContext
            context.start()
            try
            {
                return JenaUtils.getS(context.model, id, Vocabulary.HAS_MIME_TYPE)
            } finally
            {
                context.end()
            }
        }

    val inputStream: InputStream
        get()
        {
            val context = storage.readContext
            context.start()
            try
            {
                return ByteArrayInputStream(JenaUtils.listObjectsOfPropertyU(
                        context.model, id, Vocabulary.HAS_CONTENT).asLiteral().value as ByteArray)
            } finally
            {
                context.end()
            }
        }
}