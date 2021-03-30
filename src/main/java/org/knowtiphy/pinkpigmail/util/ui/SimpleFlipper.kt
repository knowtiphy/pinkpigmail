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
	val pres = HashMap<Node, () -> Unit>()
	val posts = HashMap<Node, () -> Unit>()

	private var firstFlip = true

	fun flip(node : Node)
	{
		assert(!children.isEmpty())
		assert(children.contains(node))

		//  last node in the children is on the top of the stack
		val oldNode = children.last()

		//println("FLIP from $oldNode to  $node")

		if (oldNode == node)
		{
			//		println("XXXXXXXXXXXXXXXXXXXXXXX " + children.size)
			//println("ADDDING OLD NODE " + node)
			//  TODO -- do we need to do anything?
//			children.remove(node)
//			assert(!children.contains(node))
//
//			children.add(node)
			//  the node is on top simply because it was added there
			if(firstFlip) { println("FIRST " + node); firstFlip = false; pres[node]!!.invoke();println("END FIRST " + node)}
		}
		else
		{
			//println("NORMAL " + node)
			children.remove(oldNode)
			//println("INVOKING POST " +oldNode)
			posts[oldNode]!!.invoke()
			children.add(0, oldNode)

			children.remove(node)
			//println("INVOKING PRE " + node)
			pres[node]!!.invoke()
			children.add(node)
			//println("END NORMAL " + node)

			//  add the new node to the top of the stack -- last one is o top

//			val fadeOut = FadeTransition(Duration.millis(duration))
//
//			fadeOut.node = oldNode
//			fadeOut.fromValue = 1.0
//			fadeOut.toValue = 0.0
//
//			val fadeIn = FadeTransition(Duration.millis(duration))
//			fadeIn.node = node
//			fadeIn.fromValue = 0.0
//			fadeIn.toValue = 1.0
//
//			ParallelTransition(fadeOut, fadeIn).play()
		}


	}

	fun add(node : Node, pre : () -> Unit, post : () -> Unit)
	{
		pres[node] = pre
		posts[node] = post
		children.add(0, node)
	}

	fun add(node : Node)
	{
		add(node, {}, {})
	}
}
//fun flip(node : Node)
//{
//	if (children.isEmpty()) children.add(node)
//	else
//	{
//		//  TODO -- children size can be > 1 if the animation hasn't finished when
//		//  a flip is asked for
//		//  feels like we should be able to do better than this ...
//		assert(children.size == 1) { children }
//
//		val oldNode = children.first()
//
//		println("FLIP from $oldNode to  $node")
//
//		if (oldNode != node)
//		{
//			//children.clear()
//			children.add(0, node)
//
//			val fadeOut = FadeTransition(Duration.millis(duration))
//			fadeOut.setOnFinished { assert(children.size == 1) { children }
//			}
//			fadeOut.node = oldNode
//			fadeOut.fromValue = 1.0
//			fadeOut.toValue = 0.0
//
//			val fadeIn = FadeTransition(Duration.millis(duration))
//			fadeIn.node = node
//			fadeIn.fromValue = 0.0
//			fadeIn.toValue = 1.0
//
//			ParallelTransition(fadeOut, fadeIn).play()
//		}
//	}
