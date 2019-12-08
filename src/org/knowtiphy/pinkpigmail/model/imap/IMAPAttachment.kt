package org.knowtiphy.pinkpigmail.model.imap

import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.Entity
import org.knowtiphy.pinkpigmail.model.IAttachment
import org.knowtiphy.utils.JenaUtils
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * @author graham
 */
class IMAPAttachment(id: String, val storage: IStorage, override val fileName: String, override val mimeType: String) : Entity(id), IAttachment
{
    private var path: Path? = null

    override val location: Path
        get()
        {
            if (path == null)
            {
                val fname = fileName
                //	TODO -- maybe use the mime type getAttr generate a suffix
                //  TODO - these to uses of PinkPigMail are bad
                //            if (id == null)
                //            {
                //                path = Files.createTempFile(fname == null ? "PinkPigMail" : fname, "");
                //            }
                //            else
                //            {
                //   	TODO -- will this work in Windows since org.knowtiphy.pinkpigmail.messages IDs are crazy
                path = Paths.get(Files.createTempDirectory("FOO").toString(), fname)
                Files.copy(inputStream, path!!, StandardCopyOption.REPLACE_EXISTING)
            }

            return path!!
        }

    override val inputStream: InputStream
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