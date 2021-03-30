package org.knowtiphy.pinkpigmail.util.ui

import javafx.scene.Node
import org.reactfx.EventStream

/**
 * @author graham
 */
open class LazyReplacer<T>(which : EventStream<T>, duration : Double = DEFAULT_DURATION) : Replacer(duration)
{
	private val constructors = HashMap<T, () -> Node>()
	private val cache = HashMap<T, Node>()

	init
	{
		which.subscribe {
			if (cache[it] == null)
			{
				val node = constructors[it]!!.invoke()
				cache[it] = node
			}

			flip(cache[it]!!)
		}
	}

	fun <U> addNode(key : U, constructor : () -> Node) where U : T
	{
		constructors[key] = constructor
	}

	fun <U> addNode(key : U, node : Node) where U : T
	{
		addNode(key) { node }
	}
}