package org.knowtiphy.pinkpigmail.mailview

import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker.State
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.web.WebErrorEvent
import javafx.scene.web.WebView
import org.knowtiphy.pinkpigmail.util.Fail
import org.knowtiphy.utils.OS
import org.w3c.dom.Element
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

/**
 *
 * @author graham
 */
class MailViewer : GridPane()
{
    companion object
    {
        private const val EVENT_TYPE_CLICK = "click"
//        private const val EVENT_TYPE_MOUSEOVER = "mouseover"
//        private const val EVENT_TYPE_MOUSEOUT = "mouseclick"

        private val INTERCEPT = { ev: Event ->
            if (ev.type == EVENT_TYPE_CLICK)
            {
                //	HTML sucks! Guess the real target
                val href = if ((ev.target as Element).getAttribute("href") == null)
                    ev.currentTarget.toString()
                else
                    (ev.target as Element).getAttribute("href")

                //  not sure why it has to be in a thread, but it hangs on Linux without this
                Thread { OS.open(href) }.start()

                //	TODO -- are both necessary?
                ev.preventDefault()
                ev.stopPropagation()
            }
        }
    }

    val webView = WebView()

    private var content: String? = null
    private var mimeType: String? = null

    init
    {
        addRow(0, webView)
        setHgrow(webView, Priority.ALWAYS)
        setVgrow(webView, Priority.ALWAYS)

        with(webView) {
            setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
            with(engine) {
                isJavaScriptEnabled = false
                //setOnError { event: WebErrorEvent -> Fail.failNoMessage(event.exception) }
                //	load some content to create a worker in the webView, for the next step
                loadContent("<html></html>")
                loadWorker.stateProperty().addListener { _: ObservableValue<out State>, _: State, newState: State ->
                    if (newState == State.SUCCEEDED)
                    {
                        val nodeList = webView.engine.document.getElementsByTagName("a")
                        for (i in 0 until nodeList.length)
                        {
                            (nodeList.item(i) as EventTarget).addEventListener(EVENT_TYPE_CLICK, INTERCEPT, false)
                        }
                    }
                }
            }
        }
    }

    fun reload() = webView.engine.loadContent(content, mimeType)

    fun loadContent(content: String, mimeType: String)
    {
        this.content = content
    //    println(content)
        this.mimeType = mimeType
        try
        {
           reload()
        }catch (ex : Exception)
        {
            ex.printStackTrace()
        }
    }

    //fun clear() = loadContent("", mimeType)
}