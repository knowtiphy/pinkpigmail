package org.knowtiphy.pinkpigmail.mailaccountview

import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.StringProperty
import javafx.collections.transformation.SortedList
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.Actions
import org.knowtiphy.pinkpigmail.Globals
import org.knowtiphy.pinkpigmail.cell.*
import org.knowtiphy.pinkpigmail.cell.DateCell
import org.knowtiphy.pinkpigmail.model.EmailAddress
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.model.events.FinishSyncEvent
import org.knowtiphy.pinkpigmail.model.events.MessageArrivedEvent
import org.knowtiphy.pinkpigmail.model.events.StageShowEvent
import org.knowtiphy.pinkpigmail.resources.Beep
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.Format
import org.knowtiphy.pinkpigmail.util.Functions
import org.knowtiphy.pinkpigmail.util.ui.LazyReplacer
import org.knowtiphy.pinkpigmail.util.ui.Replacer
import org.knowtiphy.pinkpigmail.util.ui.UIUtils
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.action
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.boxIt
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.button
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.later
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.maxSizeable
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.resizeable
import org.knowtiphy.pinkpigmail.util.ui.WaitSpinner
import org.reactfx.EventSource
import org.reactfx.EventStreams
import tornadofx.SmartResize
import tornadofx.remainingWidth
import java.time.ZonedDateTime
import java.util.*
import kotlin.collections.HashMap

//TODO -- review for this
//All the adapters and combinators above subscribe lazily to their inputs - they don't subscribe
// to their inputs until they themselves have at least one subscriber. When the last subscriber
// unsubscribes, they unsubscribe from the inputs as well. This behavior has two benefits:
//  MAY need to use pausable streams

class MailAccountView(account : IEmailAccount) : VBox()
{
	companion object
	{
		//  TODO -- a bit clumsy using strings, but long term could use the class of the perspective to support
		//  plugin perspectives
		private const val HORIZONTAL_P = "Horizontal"
		private const val VERTICAL_P = "Vertical"
	}

	private val model = AccountViewModel<IEmailAccount, IFolder, IMessage, String>(account)

	private val foldersRoot = TreeItem<IFolder>()
	private val treeItems = HashMap<IFolder, TreeItem<IFolder>>()

	//  the flipper that shows the current folder view to the right of the folder list
	private val folderViews = LazyReplacer(model.folderSelectedEvent)

	//  the folder space -- folder list on the left, folder views on right
	private val folderList = createFoldersList()
	private val folderSpace = resizeable(SplitPane(folderList, folderViews))

	init
	{
		setVgrow(folderSpace, Priority.ALWAYS)

		//  show the folders in the list on the left
		account.folders.values.forEach { addFolder(it) }

		//	beep on new mail arriving in the inbox
		account.events.filter(MessageArrivedEvent::class.java).cast(MessageArrivedEvent::class.java)
			.filter { it.folder.isSpecial(Vocabulary.INBOX_FOLDER) }.subscribe { Beep.beep() }

		//	beep on a sync that reveals new mail in the inbox
//		account.events.filter(FinishSyncEvent::class.java).cast(FinishSyncEvent::class.java)
//			.filter { it.synced is IFolder }.map { it.synced as IFolder }
//			.filter { it.isSpecial(Vocabulary.INBOX_FOLDER) }.subscribe {
//				if (it.unreadMessageCountProperty.get() > 0) Beep.beep()
//			}

		//	the voodoo below is to work around a JavaFX bug in setting split pane positions
		//	start with the folderList having a max width that is small
		//	then when the stage is shown change it to be resizable and set split pane positions
		//  doesn't always work, but the best kinda-solution I have found yet
		folderList.maxWidth = 100.0
		Globals.fromModel.filter(StageShowEvent::class.java)
		later {
			maxSizeable(folderList)
			folderSpace.setDividerPositions(Globals.uiSettings.getAccountSettings(account).verticalPos.get())
			Globals.uiSettings.getAccountSettings(account).verticalPos.bind(folderSpace.dividers[0].positionProperty())
		}

		children.addAll(folderSpace)
	}

	//  the tree view of folders on the left and it's toolbar

	private fun createFoldersList() : VBox
	{
		val view = resizeable(TreeView(foldersRoot))
		with(view) {
			isShowRoot = false
			setCellFactory { FolderCell() }

			//  when a folder is selected (single selection model) set the account view model category
			EventStreams.simpleChangesOf(selectionModel.selectedItems).subscribe {
				model.changeFolder(it.addedSubList[0].value)
			}
		}

		model.folderSelectedEvent.subscribe { it.sync() }
		val compose = action(Icons.compose(), { Actions.composeMail(model.account) }, Strings.COMPOSE, false)

		val toolBar = maxSizeable(HBox(button(compose)))
		with(toolBar) {
			padding = Insets(1.0, 0.0, 1.0, 0.0)
			background = Background(BackgroundFill(Color.WHITE, null, null))
		}

		setVgrow(toolBar, Priority.NEVER)
		setVgrow(view, Priority.ALWAYS)

		return resizeable(VBox(toolBar, view))
	}

	private fun addFolder(folder : IFolder)
	{
		val treeItem = TreeItem<IFolder>(folder)
		treeItems[folder] = treeItem
		foldersRoot.children.add(treeItem)
		model.addFolder(folder, listOf(VERTICAL_P, HORIZONTAL_P))

		//  TODO -- should the events actually post on the folder event stream not the account one?
		//  the replacer listens on a stream of true/false -- true means the account is synching
//		val syncView = LazyReplacer(folder.account.events.filter(FolderSyncEvent::class.java).map {
//			it.folder == folder && it::class.java.isAssignableFrom(FolderSyncStartedEvent::class.java)
//		})

		//val syncSpinner = WaitSpinner(Strings.SYNCHRONIZING_Folder)

//		with(syncView) {
//			//addNode(true, syncSpinner)
//			addNode(false) {
//				later { model.changePerspective(folder, VERTICAL_P) }
//				folderView(folder)
//			}
//		}

		val syncView = folderView(folder)
		folderViews.addNode(folder, syncView)
		later { model.changePerspective(folder, VERTICAL_P) }
	}

	//  create a view of one folder -- a flipper of different folder perspectives
	private fun folderView(folder : IFolder) : Node
	{
		val flipper = LazyReplacer<String>(model.persp(folder))
		with(flipper) {
			addNode(HORIZONTAL_P) { folderPerspective(folder, Orientation.HORIZONTAL, HORIZONTAL_P) }
			addNode(VERTICAL_P) { folderPerspective(folder, Orientation.VERTICAL, VERTICAL_P) }
		}

		return flipper
	}

	//  create a vertical/horizontal perspective of one folder
	private fun folderPerspective(folder : IFolder, orientation : Orientation, name : String) : GridPane
	{
		val messageList = messageList(folder)
		val messageView = messageView(folder, name)

		val splitPane = resizeable(SplitPane(messageList, messageView))
		splitPane.orientation = orientation

		//  each perspective has its own toolbar ...
		val toolBar = maxSizeable(toolBar(folder, orientation, name))
		//  need this to make disabling of buttons show up
		toolBar.background = Background(BackgroundFill(Color.WHITE, null, null))

		GridPane.setHgrow(toolBar, Priority.ALWAYS)
		GridPane.setHgrow(splitPane, Priority.ALWAYS)
		GridPane.setVgrow(toolBar, Priority.NEVER)
		GridPane.setVgrow(splitPane, Priority.ALWAYS)

		val perspective = GridPane()
		perspective.addRow(0, toolBar)
		perspective.addRow(1, splitPane)

		return resizeable(perspective)
	}

	//	helper methods for the buttons
	//	TODO -- they should really use events and store state in them but ah well

	@Suppress("FunctionName")
	private fun M(folder : IFolder) = model.selectionModels[folder]!!

	@Suppress("FunctionName")
	private fun MI(folder : IFolder) = M(folder).selectedItem

	@Suppress("FunctionName")
	private fun MIS(folder : IFolder) = M(folder).selectedItems

	//  create the tool bar at the top of the folder perspective

	//  TODO -- FIX THE BUTTONS WHERE WE CHANGE SELECTION!!!!

	private fun selectNext(folder : IFolder) : LinkedList<IMessage>
	{
		val messages = LinkedList(MIS(folder))
		if (messages.size == 1)
		{
			//  not sure why I can't clear and then select next ..
			//  TODO -- what happens if we select off the end? seems to just be ignored
			M(folder).clearAndSelect(M(folder).selectedIndices.first() + 1)
		}

		return messages
	}

	private fun toolBar(folder : IFolder, orientation : Orientation, name : String) : HBox
	{
		val layout = action(Icons.switchHorizontal(), {
			model.changePerspective(folder, if (orientation == Orientation.VERTICAL) HORIZONTAL_P else VERTICAL_P)
		}, if (orientation == Orientation.VERTICAL) Strings.SWITCH_HORIZONTAL else Strings.SWITCH_VERTICAL, false)

		val reply = action(Icons.reply(), { Actions.replyToMessage(MI(folder)) }, Strings.REPLY)
		val replyAll = action(Icons.replyAll(), { Actions.replyToMessage(MI(folder), true) }, Strings.REPLY_ALL)
		val forward = action(Icons.forward(), { Actions.forwardMail(MI(folder)) }, Strings.FORWARD)
		val delete = action(Icons.delete(), { Actions.deleteMessages(selectNext(folder)) }, Strings.DELETE)
		val markJunk = action(Icons.markJunk(), {
			//  move to the next message -- by default JavaFX goes back to the previous message
			Actions.markMessagesAsJunk(
				if (folder.isSpecial(Vocabulary.JUNK_FOLDER) || !folder.account.isMoveJunkMessagesToJunk) MIS(folder) else selectNext(
					folder
				)
			)
		}, Strings.MARK_JUNK)

		//  not junk? move the messages back to inbox?
		val markNotJunk =
			action(Icons.markNotJunk(), { Actions.markMessagesAsNotJunk(MIS(folder)) }, Strings.MARK_NOT_JUNK)

		val replyGroup = HBox(2.0, button(reply), button(replyAll), button(forward))
		val markGroup = HBox(2.0, button(delete), button(markJunk), button(markNotJunk))
		val middleButtons = HBox(15.0, replyGroup, markGroup)

		val toolBar = HBox(UIUtils.hSpacer(), middleButtons, UIUtils.hSpacer(), button(layout))
		toolBar.padding = Insets(1.0, 0.0, 1.0, 0.0)

		listOf(reply, replyAll, forward).forEach { action ->
			model.sel(folder, name).subscribe { action.disabledProperty().value = it.selectedItems.size != 1 }
		}

		listOf(delete, markJunk, markNotJunk).forEach { action ->
			model.sel(folder, name).subscribe { action.disabledProperty().value = it.isEmpty }
		}

		return toolBar
	}

	//  the message list for one folder perspective
	private fun messageList(folder : IFolder) : TableView<IMessage>
	{
		val view = resizeable(TableView<IMessage>())
		//  see the comment in setSelectionModel
		if (model.setSelectionModel(folder, view.selectionModel))
		{
			view.selectionModel = model.selectionModels[folder]!! as TableView.TableViewSelectionModel<IMessage>
		}

		val cellValueFactory =
			{ param : TableColumn.CellDataFeatures<IMessage, IMessage> -> ReadOnlyObjectWrapper(param.value) }

		val statusColumn = TableColumn<IMessage, IMessage>()
		with(statusColumn) {
			setCellValueFactory(cellValueFactory)
			statusColumn.setCellFactory { StatusCell() }
			prefWidth = 35.0
			maxWidth = 35.0
			minWidth = 35.0
			isSortable = false
		}

		val fromColumn = TableColumn<IMessage, IMessage>(Strings.FROM)
		with(fromColumn) {
			setCellValueFactory(cellValueFactory)
			setCellFactory { AddressCell(model.account) { it.from } }
			prefWidth = 300.0
			comparator = Functions.cmp<IMessage, EmailAddress> { it.from[0] }
		}

		val receivedCol = TableColumn<IMessage, IMessage>(Strings.RECEIVED)
		with(receivedCol) {
			setCellValueFactory(cellValueFactory)
			setCellFactory { DateCell { it.receivedOnProperty } }
			prefWidth = 200.0
			//	by default sort on the received date
			sortType = TableColumn.SortType.DESCENDING
			comparator = Functions.cmp<IMessage, ZonedDateTime> { it.receivedOnProperty.get() }
		}

		val subjectCol = TableColumn<IMessage, IMessage>(Strings.SUBJECT)
		with(subjectCol) {
			setCellValueFactory(cellValueFactory)
			setCellFactory { GeneralCell<String, StringProperty>({ it.subjectProperty }, { Format.asDate(it) }) }
			remainingWidth()
			prefWidth = 750.0
			comparator = Functions.cmp<IMessage, String> { it.subjectProperty.get() }
		}

		val sortedList = SortedList(folder.messages)
		sortedList.comparatorProperty().bind(view.comparatorProperty())

		with(view) {
			columns.addAll(statusColumn, fromColumn, subjectCol, receivedCol)
			// sort on date received
			sortOrder.add(receivedCol)
			items = sortedList
			selectionModel.selectionMode = SelectionMode.MULTIPLE
			columnResizePolicy = SmartResize.POLICY
		}

		return resizeable(view)
	}

	private fun messageView(folder : IFolder, persp : String) : Node
	{
		val messageStream = EventSource<IMessage>()

		val messageView = resizeable(MessageView(messageStream))
		val loadSpinner = WaitSpinner(Strings.LOADING_MESSAGE)
		val noMessageSelected = boxIt(Label(Strings.NO_MESSAGE_SELECTED))

		setVgrow(noMessageSelected, Priority.ALWAYS)
		setVgrow(messageView, Priority.ALWAYS)

		val messageFlipper = Replacer()

		val flipper = LazyReplacer(model.sel(folder, persp).map { it.selectedIndices.size == 1 })
		with(flipper) {
			addNode(true) { messageFlipper }
			addNode(false) { noMessageSelected }
		}

		setVgrow(flipper, Priority.ALWAYS)

		//	if the selection on the folder changes to a new single message display  it
		model.newM(folder, persp).subscribe {

			if (it == null)
			{
				println("CLEAR")
			} else
			{
				println("SELECTION CHANGE ")
				println(it.account)
				println(it.account.isDisplayMessageMarksAsRead)
				println(it.folder)
				println(it.folder.isSpecial(Vocabulary.JUNK_FOLDER))
				println("SELECTION CHANGE ")
				//	mark the message as read (don't mark junk as read)
				if (it.account.isDisplayMessageMarksAsRead && !it.folder.isSpecial(Vocabulary.JUNK_FOLDER))
				{
					it.folder.markMessagesAsRead(listOf(it))
				}

				messageFlipper.flip(loadSpinner)

				//  start a sync on the message
				val loadMessage = it.sync()

				//  start load aheads around the message
				folder.syncAhead(
					model.selectionModels[folder]!!.selectedIndices,
					(model.selectionModels[folder]!! as TableView.TableViewSelectionModel).tableView.items
				)

				//  start content loading -- single threaded executor and the use of later should ensure good
				//  behaviour if we push a new message while doing this
				//  TODO - do I believe the above comment? Probably ...
				Globals.htmlState.message = it
				Globals.service.submit {
					loadMessage.get()
					//  when the load has finished switch to the message stream view
					later {
						messageStream.push(it)
						messageFlipper.flip(messageView)
					}
				}
			}
		}

		return flipper
	}
}

//	TODO -- the commented out code is an attempt to fix a bug in JavaFX around setting
//	divider positions
//		sceneProperty().addListener { _, _, n ->
//			later {
//				try
//				{
//					folderSpace.setDividerPositions(PinkPigMail.uiSettings.getAccountSettings(account).verticalPos.get())
//				} catch (ex: Exception)
//				{
//					//  ignore
//				}
//			}
//		}
//		}
