package org.knowtiphy.pinkpigmail.util.ui

import javafx.scene.Node
import org.reactfx.EventStream

/**
 * @author graham
 */
open class LazyFlipper<T>(perspective : EventStream<T>, duration : Double = 1500.0) : SimpleFlipper(duration)
{
	private val constructors = HashMap<T, Triple<() -> Node, () -> Unit, () -> Unit>>()
	private val cache = HashMap<T, Node>()

	init
	{
		perspective.subscribe {
			if (cache[it] == null)
			{
				//  TODO -- the if... is just for testing, remove it when done
				if(constructors.containsKey(it))
				{
				val t = constructors[it]!!
				val node = t.first.invoke()
				cache[it] = node
				add(node, t.second, t.third);}
			}

			//  TODO -- the if... is just for testing, remove it when done
			if(constructors.containsKey(it))
			{
				flip(cache[it]!!)
			}
		}
	}

	fun <U> addNode(key : U, constructor : () -> Node, pre : () -> Unit, post : () -> Unit) where U : T
	{
		constructors[key] = Triple(constructor, pre, post)
	}

	fun <U> addNode(key : U, constructor : () -> Node) where U : T
	{
		addNode(key, constructor, {}, {})
	}

	fun <U> addNode(key : U, node : Node) where U : T
	{
		addNode(key) { node}
	}
}