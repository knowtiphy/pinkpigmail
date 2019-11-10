package org.knowtiphy.pinkpigmail

import org.knowtiphy.pinkpigmail.model.IAttachment
import org.knowtiphy.babbage.storage.IStorage

import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path

/**
 *
 * @author graham
 */
class OutgoingAttachment(override val location: Path) : IAttachment
{

    override val inputStream: InputStream
        @Throws(IOException::class)
        get() = FileInputStream(location.toFile())

    override val mimeType: String
        get() = ""

    override val fileName: String
        get() = location.toString()

    override fun getId(): String = ""
}
