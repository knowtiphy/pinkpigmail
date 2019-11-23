package org.knowtiphy.pinkpigmail.mailview

import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.utils.HTMLUtils
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

/**
 *
 * @author graham
 */
class CustomHttpConnection(private val handler: URLStreamHandler, private val state: HTMLState, url: URL) : URLConnection(url)
{
    override fun connect()
    {
    }

    override fun getInputStream(): InputStream
    {
        try
        {
            val message = state.message!!
            if (message.loadRemoteProperty.get() ||
                    message.mailAccount.isTrustedProvider(HTMLUtils.extractNonFilePart(url)) ||
                    message.mailAccount.isTrustedSender(message.from))
            {
                return URL(null, url.toString(), handler).openConnection().getInputStream()
            }
        } catch (_: Exception)
        {
            //	ignore
        }

        return Icons.thePig32()
    }
}