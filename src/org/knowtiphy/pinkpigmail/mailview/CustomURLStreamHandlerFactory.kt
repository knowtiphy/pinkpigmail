package org.knowtiphy.pinkpigmail.mailview

import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import java.util.*

/**
 *
 * @author graham
 */
class CustomURLStreamHandlerFactory(private val state: HTMLState) : URLStreamHandlerFactory
{
    override fun createURLStreamHandler(protocol: String): URLStreamHandler?
    {
        if (HANDLERS.containsKey(protocol))
        {
            return HANDLERS[protocol]?.invoke(state)
        }

        //  have to allow jars, even though it's dangerous
        //  TODO why can't we put the jar -> null in the map?
        return if(protocol == "jar") null else FallbackURLStreamHandler()
    }

    companion object
    {
        private val HANDLERS = HashMap<String, (HTMLState) -> URLStreamHandler>()

        init
        {
            HANDLERS["http"] = { state -> CustomHttpStreamHandler(state) }
            HANDLERS["https"] = { state -> CustomHttpStreamHandler(state) }
            HANDLERS["cid"] = { x -> CustomCIDStreamHandler(x) }
        }
    }
}