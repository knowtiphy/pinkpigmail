package org.knowtiphy.pinkpigmail.util.ui

import javafx.scene.Node
import org.reactfx.EventStream

/**
 * @author graham
 */
open class MappedReplacer<T>(which: EventStream<T>) : Replacer()
{
	private val nodes = HashMap<T, Node>()

	init
	{
		//	TODO -- have to unsubscribe somehow when not being used any more
		which.subscribe() { flip(nodes[it]!!) }
	}

	fun addNode(key: T, node: Node)
	{
		nodes[key] = node
		children.add(node)
		flip(node)
	}
}