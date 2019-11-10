package org.knowtiphy.pinkpigmail.mailview

import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

/**
 *
 * @author graham
 */
class CustomHttpStreamHandler(private val handler: URLStreamHandler, private val state: HTMLState) : URLStreamHandler()
{
    override fun openConnection(url: URL): URLConnection
    {
        return CustomHttpConnection(handler, state, url)
    }
}
