package org.knowtiphy.pinkpigmail.util.ui

import javafx.beans.property.Property
import javafx.scene.Node

/**
 * @author graham
 */
open class MappedReplacer<T>(whichProperty: Property<T>) : Replacer()
{
     val nodes = HashMap<T, Node>()

    init
    {
        whichProperty.addListener { _, _, newValue -> flip(nodes[newValue]!!) }
    }

    fun addNode(key: T, node: Node)
    {
        nodes[key] = node;
        children.add(node)
        flip(node)
    }
}