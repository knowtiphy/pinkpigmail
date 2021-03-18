package org.knowtiphy.pinkpigmail.mailview

import com.squareup.okhttp.OkHttpClient
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.utils.HTMLUtils
import java.io.InputStream
import java.net.URL
import java.net.URLConnection

/**
 *
 * @author graham
 */
class CustomHttpConnection(private val state: HTMLState, url: URL) : URLConnection(url)
{
    companion object
    {
        private val client = OkHttpClient()
    }

    override fun connect()
    {
    }

    override fun getInputStream(): InputStream
    {
        try
        {
            val message = state.message!!
            if (message.loadRemoteProperty.get() ||
                    message.account.isTrustedProvider(HTMLUtils.extractNonFilePart(url)) ||
                    message.account.isTrustedSender(message.from))
            {
                return client.open(url).inputStream
            }
        } catch (_: Exception)
        {
            //	ignore
        }

        return Icons.thePig32()
    }
}