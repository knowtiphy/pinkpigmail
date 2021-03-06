package org.knowtiphy.pinkpigmail.util.ui

import javafx.scene.Node
import javafx.scene.layout.StackPane

/**
 * @author graham
 */
open class Replacer : StackPane()
{
    fun flip(node: Node)
    {
        children.remove(node)
        children.add(node)
    }
}

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