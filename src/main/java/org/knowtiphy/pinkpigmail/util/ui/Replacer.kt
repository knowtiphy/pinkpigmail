package org.knowtiphy.pinkpigmail.util.ui

import javafx.animation.FadeTransition
import javafx.animation.ParallelTransition
import javafx.scene.Node
import javafx.scene.layout.StackPane
import javafx.util.Duration

/**
 * @author graham
 */
open class Replacer(private val duration : Double = DEFAULT_DURATION) : StackPane()
{
	companion object
	{
		const val DEFAULT_DURATION = 500.0
	}

	private var initial : Node? = null

	fun flip(newNode : Node)
	{
		if (children.size == 0 || (children.size == 1 && children.first() == initial))
		{
			children.add(newNode)
		}
		else
		{
			val oldNode = children.last()

			//  TODO -- oldNode could be equal to node? If the flip to X (e.g on a mouse click), start some long running
			//  action that will flip to Y, but before it finishes they flip to X again (e.g by a mouse click)

			if (oldNode != newNode)
			{
				children.add(newNode)

				//  we have to remove immediately in case they flip to a node, and before it is removed at the end of the
				//``animation, it gets added again
				//  TODO -- this should mean we are animating a node which isn't in the scene graph!!!

				children.remove(oldNode)

				val fadeOut = FadeTransition(Duration.millis(duration))
				with(fadeOut) {
					node = oldNode
					//setOnFinished { children.remove(oldNode) }
					fromValue = 1.0
					toValue = 0.0
				}

				val fadeIn = FadeTransition(Duration.millis(duration))
				with(fadeIn) {
					node = newNode
					fromValue = 0.0
					toValue = 1.0
				}

				ParallelTransition(fadeOut, fadeIn).play()
			}
		}
	}

	//  set an initial node that does not participate in the flipping process
	fun setInitial(node : Node)
	{
		children.clear()
		children.add(node)
		initial = node
	}
}