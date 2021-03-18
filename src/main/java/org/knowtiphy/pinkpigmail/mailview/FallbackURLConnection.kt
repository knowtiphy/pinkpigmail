package org.knowtiphy.pinkpigmail.mailview

import org.knowtiphy.pinkpigmail.resources.Icons
import java.io.InputStream
import java.net.URL
import java.net.URLConnection

/**
 *
 * @author graham
 */
class FallbackURLConnection(url: URL) : URLConnection(url)
{
    override fun connect()
    {
    }

    override fun getInputStream(): InputStream
    {
        return Icons.thePig32()
    }
}
