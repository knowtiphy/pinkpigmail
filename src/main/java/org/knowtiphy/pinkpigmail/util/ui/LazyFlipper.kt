package org.knowtiphy.pinkpigmail.util.ui

import javafx.scene.Node
import org.reactfx.EventStream

/**
 * @author graham
 */
open class LazyFlipper<T>(perspective : EventStream<T>, duration : Double = 1500.0) : SimpleFlipper(duration)
{
	private val constructors = HashMap<T, () -> Node>()
	private val cache = HashMap<T, Node>()

	init
	{
		perspective.subscribe {
			if (cache[it] == null) cache[it] = constructors[it]!!.invoke()
			flip(cache[it]!!)
		}
	}

	fun <U> addNode(key : U, constructor : () -> Node) where U : T
	{
		constructors[key] = constructor
	}

	fun <U> addNode(key : U, node : Node) where U : T
	{
		constructors[key] = { node }
	}
}