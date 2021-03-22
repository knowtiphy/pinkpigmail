package org.knowtiphy.pinkpigmail.mailaccountview

import javafx.collections.FXCollections
import javafx.scene.control.MultipleSelectionModel
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.model.MessageFrame
import org.knowtiphy.pinkpigmail.util.MatchFunction
import org.reactfx.EventSource
import org.reactfx.EventStream
import org.reactfx.EventStreams

//  the view-model for the account view
//  A is the type of the account (email, caldav, carddav)
//  C is the type of the accounts entries (folder, calendar, address book)
//  E is the type of the base entities (messages, events, cards)

class AccountViewModel<A, C, E>(val account: A)
{
	//	a message has been shown
	val messageShown = EventSource<IMessage>()

	val loadAhead =
		MatchFunction<IMessage, Collection<Collection<IMessage>>, MessageFrame> { msg, surroundingMessages ->
			MessageFrame(msg, surroundingMessages)
		}

	init
	{
		//	for each message we show that has a matching frame, start a loadhead of the frame
		messageShown.map(loadAhead).filter { it != null }.subscribe { it!!.loadAhead() }
	}

	//  the per folder selection models
	val selectionModels = HashMap<C, MultipleSelectionModel<E>>()

	//	the current perspective
	//	TODO -- change the buttons to use events
	private var currentPerspective = HashMap<C, String?>()

	//	on event stream per folder for changes in that folder's perspective
	val perspective = HashMap<C, EventSource<String>>()

	//	event stream for when a folder is selected
	val folderSelectedEvent = EventSource<C>()

	//	event stream for when a folder's selection indices change
	//	seems redundant given selectionModels?
	val selection = HashMap<C, EventStream<MultipleSelectionModel<E>>>()

	//	add a folder to the model making
	//	- a selecton change event source for the folder
	//	- perspective change event source for the folder
	//	- a current perspective property

	fun addCategory(category: C)
	{
		assert(!selection.containsKey(category))
		assert(!perspective.containsKey(category))
		assert(!currentPerspective.containsKey(category))
		assert(!selectionModels.containsKey(category))

		selection[category] = EventSource()
		perspective[category] = EventSource()
	}

	//	change the perspective for a folder
	fun changePerspective(folder: C, name: String)
	{
		assert(perspective.containsKey(folder))
		currentPerspective[folder] = name
		perspective[folder]!!.push(name)
	}

	fun changeFolder(folder: C)
	{
		//currentFolder = folder
		folderSelectedEvent.push(folder)
	}

	//	this is nasty -- JavaFX insists on having it's fucking models bundled with its views -- braindead
	//	so we hve to force it to share the selection model between table views

	fun setSelection(folder: C, selectionModel: MultipleSelectionModel<E>)
	{
		selectionModels[folder] = selectionModel
		//Bindings.bindContent(sharedSelection[folder], selectionModel.selectedIndices)
		selection[folder] = EventStreams.changesOf(selectionModel.selectedIndices).map { selectionModels[folder]!! }
	}

	fun isCurrentPerspective(folder: C, name: String) = name == currentPerspective[folder]
}
