package org.knowtiphy.pinkpigmail.util.ui

import javafx.scene.Node
import javafx.scene.layout.StackPane
import org.reactfx.EventStream

/**
 * @author graham
 */
open class LazyMappedReplacer<T>(perspective: EventStream<T>, private val duration : Double = 3000.0) :
		Replacer(duration)
{
	private val constructors = HashMap<T, () -> Node>()
	private val cache = HashMap<T, Node>()

	init
	{
		perspective.subscribe {
			if (cache[it] == null)
				cache[it] = constructors[it]!!.invoke()
			flip(cache[it]!!)
		}
	}

	fun addNode(key: T, constructor: () -> Node)
	{
		constructors[key] = constructor
	}

	fun addNode(key: T, node : Node)
	{
		constructors[key] = { node }
	}
}