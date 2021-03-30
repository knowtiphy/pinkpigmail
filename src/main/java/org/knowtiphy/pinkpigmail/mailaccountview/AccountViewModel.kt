package org.knowtiphy.pinkpigmail.mailaccountview

import javafx.collections.ListChangeListener
import javafx.scene.control.MultipleSelectionModel
import org.reactfx.EventSource

//  the view-model for the account view
//  A is the type of the account (email, caldav, carddav)
//  C is the type of the accounts entries (folder, calendar, address book)
//  E is the type of the base entities (messages, events, cards)
//  P is the type used to identify perspectives

class AccountViewModel<A, C, E, P>(val account : A)
{
	//	the current perspective -- needed to only deliver events to the current perspective
	private var currentPerspective = HashMap<C, P?>()

	//  the per folder selection models
	val selectionModels = HashMap<C, MultipleSelectionModel<E>>()

	//	event stream for when a folder is selected
	val folderSelectedEvent = EventSource<C>()

	//	one event stream per folder for changes in that folder's perspective
	private val perspective = HashMap<C, EventSource<P>>()

	//	per folder per perspective stream for when a folder's selection indices change
	//  changes are only delivered to the current perspective
	private val selection = HashMap<C, HashMap<P, EventSource<MultipleSelectionModel<E>>>>()

	//	per folder per perspective convenience streams giving the actual selected message(s)
	//	when a folder's selection changes
	//  changes are only delivered to the current perspective
	private val newMessage = HashMap<C, HashMap<P, EventSource<E>>>()
	private val newMessages = HashMap<C, HashMap<P, EventSource<List<E>>>>()

	//  getter methods for event source
	fun sel(folder : C, persp : P) : EventSource<MultipleSelectionModel<E>> = selection[folder]!![persp]!!
	fun persp(folder : C) : EventSource<P> = perspective[folder]!!
	fun newM(folder : C, persp : P) : EventSource<E> = newMessage[folder]!![persp]!!

	private fun newMs(folder : C, persp : P) : EventSource<List<E>> = newMessages[folder]!![persp]!!

	//	add a folder to the model making
	fun addFolder(folder : C, pspecs : List<P>)
	{
		assert(!selection.containsKey(folder))
		assert(!perspective.containsKey(folder))
		assert(!currentPerspective.containsKey(folder))
		assert(!selectionModels.containsKey(folder))
		assert(!newMessage.containsKey(folder))
		assert(!newMessage.containsKey(folder))

		perspective[folder] = EventSource()
		selection[folder] = HashMap()
		newMessage[folder] = HashMap()
		newMessages[folder] = HashMap()

		pspecs.forEach {
			selection[folder]!![it] = EventSource()
			newMessage[folder]!![it] = EventSource()
			newMessages[folder]!![it] = EventSource()
		}
	}

	//	change the perspective for a folder
	fun changePerspective(folder : C, persp : P)
	{
		assert(perspective.containsKey(folder))
		currentPerspective[folder] = persp
		persp(folder).push(persp)
		//  this is a little clumsy .. but in the event way of things its necessary ...
		pushChanges(folder)
	}

	//	change the model's current folder
	fun changeFolder(folder : C)
	{
		folderSelectedEvent.push(folder)
	}

	//  We want to share a selection model between different perspectives
	//  One way would be to bind each selection model's indices and items lists
	//  to a list (possibly observable), and then on change of perspective push new
	//  message events?
	//  Or we do the hack below -- share the selection model of the first created view
	//  between all views :)
	fun setSelectionModel(folder : C, model : MultipleSelectionModel<E>) : Boolean
	{
		if (!selectionModels.containsKey(folder))
		{
			selectionModels[folder] = model
			model.selectedIndices.addListener { _ : ListChangeListener.Change<*> -> pushChanges(folder) }
			return false
		}

		return true
	}

	//	push changes on various event streams -- have to do this initially when a folder is
	//	added to setup the initial state of folder view, and when a perspective is changed
	//	to inform the new perspective of the value of the streams (since we don't push changes
	//  in stuff to anything other than the current perspective)
	private fun pushChanges(folder : C)
	{
		val selectionModel = selectionModels[folder]!!

		if (selectionModel.selectedItems.size == 1)
		{
			newM(folder, currentPerspective[folder]!!).push(selectionModel.selectedItem)
		}
		else
		{
			newMs(folder, currentPerspective[folder]!!).push(selectionModel.selectedItems)
		}

		sel(folder, currentPerspective[folder]!!).push(selectionModel)
	}
}