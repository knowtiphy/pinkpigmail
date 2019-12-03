package org.knowtiphy.pinkpigmail

import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import org.reactfx.Change
import org.reactfx.EventSource
import org.reactfx.EventStream
import org.reactfx.EventStreams

//  the view-model for the account view
class AccountViewModel<A, C, E>(val account: A)
{
    val currentEntityProperty = SimpleObjectProperty<C>()

    private val categoryViewModels = HashMap<C, CategoryViewModel<C, E>>()

    //  event sources -- the selected category or entity changes
    val categorySelected: EventStream<Change<C>> = EventStreams.changesOf(currentEntityProperty)
    val entitySelected: EventSource<CategoryViewModel<C, E>> = EventSource()

    init
    {
        categorySelected.subscribe { entitySelected.push(currentCategoryViewModel()) }
    }

    fun currentCategoryViewModel(): CategoryViewModel<C, E> = categoryViewModels[currentEntityProperty.get()]!!

    fun addCategoryViewModel(category: C, model: CategoryViewModel<C, E>)
    {
        categoryViewModels[category] = model
    }

    fun currentEntities(): ObservableList<out E>
    {
        assert(currentCategoryViewModel().selectionModel!!.selectedItems.isNotEmpty())
        return currentCategoryViewModel().selectionModel!!.selectedItems
    }

    fun currentEntity(): E
    {
        assert(currentCategoryViewModel().selectionModel!!.selectedItems.size == 1)
        return currentCategoryViewModel().selectionModel!!.selectedItem
    }
}