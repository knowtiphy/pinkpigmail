package org.knowtiphy.pinkpigmail.mailaccountview

import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ListChangeListener
import javafx.scene.control.TableView
import org.reactfx.Change
import org.reactfx.EventSource
import org.reactfx.EventStream
import org.reactfx.EventStreams

//  the view-model for the account view
//  A is the type of the account (email, caldav, carddav)
//  C is the type of the accounts entries (folder, calendar, address book)
//  E is the type of the base entities (messages, events, cards)

class AccountViewModel<A, C, E>(val account: A)
{
	//  the currently selected category
	private val currentCategory = SimpleObjectProperty<C>()
	//  one tableview selection model per category
	private val tableViewSelectionModels = HashMap<C, TableView.TableViewSelectionModel<E>>()
	//	one selection model per categoy
	private val selectionModels = HashMap<C, EntitySelection<C, E>>()
	//  one current perspective for each category
	private val perspectives = HashMap<C, SimpleObjectProperty<String>>()

	//  event sources -- the selected category, selection model per category, perspective per category

	val category: EventStream<Change<C>> = EventStreams.changesOf(currentCategory)
	val selection = HashMap<C, EventSource<EntitySelection<C, E>>>()
	val perspective = HashMap<C, EventStream<Change<String>>>()

	fun currentCategoryProperty(): ReadOnlyObjectProperty<C> = currentCategory
	fun currentSelection(category: C) = selectionModels[category]!!
	fun currentTableViewSelectionModel(category: C) = tableViewSelectionModels[category]!!
	fun isCurrentPerspective(category: C, name: String) = perspectives[category]!!.get() == name

	fun addCategory(category: C)
	{
		selection[category] = EventSource()
		perspectives[category] = SimpleObjectProperty()
		perspective[category] = EventStreams.changesOf(perspectives[category])
	}

	fun changeCategory(category: C)
	{
		currentCategory.set(category)
	}

	private fun outputSelection(category: C, sel: EntitySelection<C, E>)
	{
		selectionModels[category] = sel
		selection[category]!!.push(sel)
	}

	//	this is nasty -- JavaFX insists on having it's fucking models bundled with its views -- braindead
	//	so we have to add a listener to the selection model of a table view and propagate changes from it
	//	through to our per category event stream
	//	we also have to share the selection model between table views

	fun bindSelectionModel(category: C, model: TableView.TableViewSelectionModel<E>): TableView.TableViewSelectionModel<E>?
	{
		return if (tableViewSelectionModels[category] == null)
		{
			tableViewSelectionModels[category] = model
			//	TODO -- how do I know that selectedIndices changes in synch with selectedItems?
			model.selectedIndices.addListener { _: ListChangeListener.Change<out Int> ->
				outputSelection(category, EntitySelection<C, E>(category, model.selectedIndices, model.selectedItems))
			}
			null
		} else
			tableViewSelectionModels[category]
	}

	fun changePerspective(category: C, name: String)
	{
		//	must do these in this order since the change in perspective will cause a perspective to be created,
		//	and hence the selection model to be created (see the comment in bindSelection)
		perspectives[category]!!.value = name
		val es = tableViewSelectionModels[category]!!
		//	TODO -- this is hacky crap, why "change" selection on a perspective change?
		outputSelection(category, EntitySelection(category, es.selectedIndices, es.selectedItems))
	}
}