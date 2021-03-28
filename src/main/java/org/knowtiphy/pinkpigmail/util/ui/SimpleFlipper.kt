package org.knowtiphy.pinkpigmail.util.ui

import javafx.animation.FadeTransition
import javafx.animation.ParallelTransition
import javafx.scene.Node
import javafx.scene.layout.StackPane
import javafx.util.Duration

/**
 * @author graham
 */
open class SimpleFlipper(private val duration : Double = 1500.0) : StackPane()
{
	fun flip(node : Node)
	{
		if (children.isEmpty()) children.add(node)
		else
		{
			//  TODO -- children size can be > 1 if the animation hasn't finished when
			//  a flip is asked for
			//  feels like we should be able to do better than this ...
			assert(children.size == 1) { children }

			val oldNode = children.first()

			println("FLIP from $oldNode to  $node")

			if (oldNode != node)
			{
				children.clear()
				children.add(0, node)

				val fadeOut = FadeTransition(Duration.millis(duration))
				fadeOut.setOnFinished { assert(children.size == 1) { children }
				}
				fadeOut.node = oldNode
				fadeOut.fromValue = 1.0
				fadeOut.toValue = 0.0

				val fadeIn = FadeTransition(Duration.millis(duration))
				fadeIn.node = node
				fadeIn.fromValue = 0.0
				fadeIn.toValue = 1.0

				ParallelTransition(fadeOut, fadeIn).play()
			}
		}
	}
}