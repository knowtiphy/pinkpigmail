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

        //	have to allow this through to load jars, but that is dangerous
        return if (state.isAllowJars && protocol == "jar")
        {
            null
        } else FallbackURLStreamHandler()
    }

    companion object
    {
        private var SUN_HTTP_HANDLER: URLStreamHandler? = null

        init
        {
            try
            {
                SUN_HTTP_HANDLER = Class.forName("sun.net.www.protocol.http.Handler").getDeclaredConstructor().newInstance() as URLStreamHandler
            } catch (ex: ClassNotFoundException)
            {
                SUN_HTTP_HANDLER = null
            } catch (ex: IllegalAccessException)
            {
                SUN_HTTP_HANDLER = null
            } catch (ex: InstantiationException)
            {
                SUN_HTTP_HANDLER = null
            }
        }

        private var SUN_HTTPS_HANDLER: URLStreamHandler? = null

        init
        {
            try
            {
                SUN_HTTPS_HANDLER =
                        Class.forName("sun.net.www.protocol.https.Handler").getDeclaredConstructor().newInstance() as URLStreamHandler
            } catch (ex: ClassNotFoundException)
            {
                SUN_HTTPS_HANDLER = null
            } catch (ex: IllegalAccessException)
            {
                SUN_HTTPS_HANDLER = null
            } catch (ex: InstantiationException)
            {
                SUN_HTTPS_HANDLER = null
            }
        }

        private val HANDLERS = HashMap<String, (HTMLState) -> URLStreamHandler>()

        init
        {
            HANDLERS["http"] = { state -> CustomHttpStreamHandler(SUN_HTTP_HANDLER!!, state) }
            HANDLERS["https"] = { state -> CustomHttpStreamHandler(SUN_HTTPS_HANDLER!!, state) }
            HANDLERS["cid"] = { x -> CustomCIDStreamHandler(x) }
        }
    }
}
