package org.knowtiphy.pinkpigmail.util.ui

import javafx.animation.FadeTransition
import javafx.animation.ParallelTransition
import javafx.scene.Node
import javafx.scene.layout.StackPane
import javafx.util.Duration

/**
 * @author graham
 */
open class Replacer(private val duration : Double = 3000.0) : StackPane()
{
	fun flip(node: Node)
	{
		if (children.isEmpty())
			children.add(node)
		else
		{
			assert( children.size == 1){ children}
			val oldNode = children.first()
			//	not completely sure how this happens - but it makes it simpler for others to use
			if(oldNode != node)
			{

				children.add(0, node)

				val fadeOut = FadeTransition(Duration.millis(duration))
				fadeOut.setOnFinished { children.remove(oldNode) }
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