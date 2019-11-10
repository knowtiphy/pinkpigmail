package org.knowtiphy.pinkpigmail.util

import javafx.beans.property.Property
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Region

/**
 * @author graham
 */
open class Flipper<T>(val whichProperty: Property<T>) : BorderPane()
{
    private val nodes = HashMap<T, Region>()

    init
    {
        center = Pane()
        UIUtils.resizable(center as Pane)
        whichProperty.addListener { _, _, newValue ->
            center = nodes[newValue]
        }
    }

    fun addNode(name: T, node: Region)
    {
        nodes[name] = node
    }
}