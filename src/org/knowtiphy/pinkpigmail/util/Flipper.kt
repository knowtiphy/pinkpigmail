package org.knowtiphy.pinkpigmail.util

import javafx.scene.Node
import javafx.scene.layout.StackPane

/**
 * @author graham
 */
open class Flipper() : StackPane()
{
    fun flip(node: Node)
    {
        children.remove(node)
        children.add(node)
    }

    fun rotate()
    {
        flip(children.last())
    }
}