package org.knowtiphy.pinkpigmail.util.ui

import javafx.beans.property.ReadOnlyObjectProperty
import javafx.scene.Node

/**
 * @author graham
 */
open class MappedReplacer<T>(whichProperty : ReadOnlyObjectProperty<T>) : Replacer()
{
   // val whichProperty = SimpleObjectProperty<T>()

    private val nodes = HashMap<T, Node>()

    init
    {
        whichProperty.addListener { _, _, newValue -> flip(nodes[newValue]!!) }
    }

    fun addNode(key: T, node: Node)
    {
        nodes[key] = node
        children.add(node)
        flip(node)
    }
}