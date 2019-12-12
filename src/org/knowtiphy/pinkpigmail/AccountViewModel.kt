package org.knowtiphy.pinkpigmail

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.TreeItem
import org.reactfx.Change
import org.reactfx.EventStream
import org.reactfx.EventStreams

//  the view-model for the account view -- so the account and its folders/calendars/etc
class AccountViewModel<A, C, E>(val account: A)
{
    //  TODO -- shouldn't be public -- Flipper mess
    private val selectedCategory = SimpleObjectProperty<TreeItem<C>>()

    //  one selection model per category
    private val selectionModels = HashMap<C, MultipleSelectionModel<E>?>()

    //  event sources -- the selected category
    val categorySelected: EventStream<Change<TreeItem<C>>> = EventStreams.changesOf(selectedCategory)

    fun setCategory(category: TreeItem<C>)
    {
        println("SETTING CATEGORY FOR " + category.value)
        selectedCategory.set(category)
    }

    fun setSelectionModel(category: C, model: MultipleSelectionModel<E>)
    {
        selectionModels[category] = model
    }

    fun getSelectionModel(category: C) = selectionModels[category]!!
    fun isNullSelectionModel(category: C) = selectionModels[category] == null
}