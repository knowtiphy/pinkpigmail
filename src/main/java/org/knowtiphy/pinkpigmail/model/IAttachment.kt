package org.knowtiphy.pinkpigmail.model

import org.knowtiphy.babbage.storage.exceptions.StorageException
import org.knowtiphy.owlorm.javafx.IEntity
import java.awt.Desktop
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * @author graham
 */
interface IAttachment : IEntity, Comparable<IAttachment>
{
  //  val inputStream: InputStream

    val location: Path

    val mimeType: String?

    val fileName: String

    @Throws(IOException::class, StorageException::class)
    fun open()
    {
        val path = location
        Runtime.getRuntime().addShutdownHook(Thread {
            try
            {
                Files.delete(path)
            } catch (`$`: IOException)
            {
                //	ignore
            }
        })

        //  not sure why it has to be in a thread, but it hangs on Linux without this
        if (Desktop.isDesktopSupported())
        {
            Thread {
                try
                {
                    Desktop.getDesktop().open(path.toFile())
                } catch (e1: IOException)
                {
                    e1.printStackTrace()
                }
            }.start()
        }
    }

    override fun compareTo(other: IAttachment): Int
    {
        return try
        {
            fileName.compareTo(other.fileName)
        } catch (_: NullPointerException)
        {
            //  getFileName can return null
            0
        }
    }
}
