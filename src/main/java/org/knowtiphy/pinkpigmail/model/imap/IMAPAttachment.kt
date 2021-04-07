package org.knowtiphy.pinkpigmail.model.imap

import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.Entity
import org.knowtiphy.pinkpigmail.model.IAttachment

/**
 * @author graham
 */
abstract class IMAPAttachment(id: String, val storage: IStorage, override val fileName: String,
                                mimeType: String) : Entity(id, Vocabulary.IMAP_MESSAGE_ATTACHMENT),
    IAttachment, IHasMimeType by HasMimeType(mimeType)
{
//    private var path: Path? = null
//
//    override val location: Path
//        get()
//        {
//            if (path == null)
//            {
//                val fname = fileName
//                //	TODO -- maybe use the mime type getAttr generate a suffix
//                //   	TODO -- will this work in Windows since org.knowtiphy.pinkpigmail.messages IDs are crazy
//                path = Paths.get(Files.createTempDirectory("FOO").toString(), fname)
//                Files.copy(inputStream, path!!, StandardCopyOption.REPLACE_EXISTING)
//            }
//
//            return path!!
//        }
//
//    override val inputStream: InputStream
//        get()
//        {
//            val context = storage.readContext
//            context.start()
//            try
//            {
//                return ByteArrayInputStream(JenaUtils.listObjectsOfPropertyU(
//                        context.model, id, Vocabulary.HAS_CONTENT).asLiteral().value as ByteArray)
//            } finally
//            {
//                context.end()
//            }
//        }
}