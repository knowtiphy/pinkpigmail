package org.knowtiphy.pinkpigmail.mailview

import org.knowtiphy.pinkpigmail.Fail
import org.knowtiphy.pinkpigmail.Mime
import org.knowtiphy.pinkpigmail.resources.Icons
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLConnection

/**
 *
 * @author graham
 */
class CustomCIDConnection(private val state: HTMLState, url: URL) : URLConnection(url)
{
    @Throws(IOException::class)
    override fun connect()
    {
    }

    @Throws(IOException::class)
    override fun getInputStream(): InputStream
    {
        try
        {
            val part = state.message!!.cidMap[getURL()]
            return if (part != null && Mime.isImage(part.mimeType)) part.inputStream else Icons.thePig32()
        } catch (ex: Exception)
        {
            ex.printStackTrace(System.err)
            Fail.failNoMessage(ex)
        }

        return Icons.thePig32()
    }
}
