package org.knowtiphy.pinkpigmail.mailaccountview

class EntitySelection<C, E>(val category : C, val selectedIndices : Collection<Int>, val selectedItems : List<E>)
{
	fun size() = selectedIndices.size

	fun isEmpty() = selectedIndices.isEmpty()

	fun selectedItem() = selectedItems.first()

	override fun toString(): String
	{
		return "EntitySelection(category=$category, selectedIndices=$selectedIndices, selectedItems=$selectedItems)"
	}
}