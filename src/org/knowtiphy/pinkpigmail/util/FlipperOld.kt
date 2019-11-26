package org.knowtiphy.pinkpigmail.util

import javafx.beans.property.Property
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane

/**
 * @author graham
 */
open class FlipperOld<T>(val whichProperty: Property<T>, duration: Double = 500.0) : StackPane()
{
    private val nodes = HashMap<T, Region>()

    init
    {
        whichProperty.addListener { _, _, newValue ->
            val center = nodes[newValue]
            val paneToRemove = children[0]
            if (paneToRemove != center)
            {
                children.add(0, center)
                children.remove(paneToRemove)
//                val fadeOut = FadeTransition(Duration.millis(duration))
//                fadeOut.setOnFinished { children.remove(paneToRemove) }
//                fadeOut.node = paneToRemove
//                fadeOut.fromValue = 1.0
//                fadeOut.toValue = 0.0
//
//                val fadeIn = FadeTransition(Duration.millis(duration))
//                fadeIn.node = center
//                fadeIn.fromValue = 0.0
//                fadeIn.toValue = 1.0
//
//                ParallelTransition(fadeOut, fadeIn).play()
            }
        }
    }

    fun addNode(name: T, node: Region)
    {
        nodes[name] = node
        children.clear()
        children.add(node)
    }
}