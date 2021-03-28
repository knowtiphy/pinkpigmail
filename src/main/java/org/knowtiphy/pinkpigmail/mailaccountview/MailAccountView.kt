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
import org.knowtiphy.pinkpigmail.PinkPigMail
import org.knowtiphy.pinkpigmail.cell.*
import org.knowtiphy.pinkpigmail.cell.DateCell
import org.knowtiphy.pinkpigmail.model.EmailAddress
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.model.events.FolderSyncEvent
import org.knowtiphy.pinkpigmail.model.events.FolderSyncStartedEvent
import org.knowtiphy.pinkpigmail.model.events.MessageArrivedEvent
import org.knowtiphy.pinkpigmail.model.events.StageShowEvent
import org.knowtiphy.pinkpigmail.resources.Beep
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.Format
import org.knowtiphy.pinkpigmail.util.Functions
import org.knowtiphy.pinkpigmail.util.ui.LazyFlipper
import org.knowtiphy.pinkpigmail.util.ui.SimpleFlipper
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

class MailAccountView(account : IEmailAccount) : VBox()
{
	companion object
	{
		private const val HORIZONTAL_P = "Horizontal"
		private const val VERTICAL_P = "Vertical"
	}

	private val model = AccountViewModel<IEmailAccount, IFolder, IMessage, String>(account)
	private val foldersRoot = TreeItem<IFolder>()
	private val treeItems = HashMap<IFolder, TreeItem<IFolder>>()

	//  the folder flipper that shows the current folder view to the right of the folder list
	private val folderViewFlipper = LazyFlipper(model.folderSelectedEvent)

	//  the folder space -- folder list on the left, folder view on right
	private val folderList = createFoldersList()
	private val folderSpace = resizeable(SplitPane(folderList, folderViewFlipper))

	init
	{
		setVgrow(folderSpace, Priority.ALWAYS)
		children.addAll(folderSpace)

		//  show the folders in the list on the left
		account.folders.values.forEach { addFolder(it) }

		//	beep on new mail in the inbox
		account.events.filter(MessageArrivedEvent::class.java)
			.filter { (it as MessageArrivedEvent).folder.isSpecial(Vocabulary.INBOX_FOLDER) }.subscribe { Beep.beep() }

		//	the voodoo below is to work around a JavaFX bug in setting split pane positions
		//	start with the folderList having a max width that is small
		//	then when the stage is shown change it to be resizable and set split pane positions
		//  doesn't always work, but the best "solution" I have found yet
		folderList.maxWidth = 100.0
		PinkPigMail.fromModel.filter(StageShowEvent::class.java)
		later {
			maxSizeable(folderList)
			folderSpace.setDividerPositions(PinkPigMail.uiSettings.getAccountSettings(account).verticalPos.get())
			PinkPigMail.uiSettings.getAccountSettings(account).verticalPos.bind(folderSpace.dividers[0].positionProperty())
		}
	}

	private fun addFolder(folder : IFolder)
	{
		val treeItem = TreeItem<IFolder>(folder)
		treeItems[folder] = treeItem
		foldersRoot.children.add(treeItem)
		model.addFolder(folder, listOf(VERTICAL_P, HORIZONTAL_P))

		//	TODO -- have to make the folderView for the change perspective to work
		val fView = folderView(folder)

		val syncView = LazyFlipper(folder.account.events.filter(FolderSyncEvent::class.java).map {
			it::class.java.isAssignableFrom(FolderSyncStartedEvent::class.java)
		})

		with(syncView) {
			addNode(true) { WaitSpinner(Strings.SYNCHRONIZING_Folder) }
			addNode(false) { fView }
		}

		folderViewFlipper.addNode(folder, syncView)

		//	TODO -- get the initial perspective from some sort of UI settings
		model.changePerspective(folder, VERTICAL_P)
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

	private fun toolBar(folder : IFolder, orientation : Orientation, name : String) : HBox
	{
		val layout = action(Icons.switchHorizontal(), {
			model.changePerspective(folder, if (orientation == Orientation.VERTICAL) HORIZONTAL_P else VERTICAL_P)
		}, if (orientation == Orientation.VERTICAL) Strings.SWITCH_HORIZONTAL else Strings.SWITCH_VERTICAL, false)

		val reply = action(Icons.reply(), { Actions.replyToMessage(MI(folder)) }, Strings.REPLY)
		val replyAll = action(Icons.replyAll(), { Actions.replyToMessage(MI(folder), true) }, Strings.REPLY_ALL)
		val forward = action(Icons.forward(), { Actions.forwardMail(MI(folder)) }, Strings.FORWARD)
		val delete = action(Icons.delete(), {
			//  move to the next message -- by default JavaFX goes back to the previous message
			Actions.deleteMessages(MIS(folder))
			M(folder).selectNext()
		}, Strings.DELETE)
		val markJunk = action(Icons.markJunk(), {
			//  move to the next message -- by default JavaFX goes back to the previous message
			Actions.markMessagesAsJunk(MIS(folder))
			M(folder).selectNext()
		}, Strings.MARK_JUNK)
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

	//  create a vertical/horizontal perspective of one folder
	private fun folderPerspective(folder : IFolder, orientation : Orientation, name : String) : GridPane
	{
		//  slightly hacky to have the message property between the event and the actual message
		//  view -- but allows the load ahead strategy to be determined here and the message
		//  view simply worries about showing messages

		val messageList = messageList(folder)
		val messageView = messageView(folder, name)

		val splitPane = resizeable(SplitPane(messageList, messageView))
		splitPane.orientation = orientation

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

	//  create a view of one folder -- a flipper of different folder perspectives
	private fun folderView(folder : IFolder) : Node
	{
		val flipper = LazyFlipper<String>(model.persp(folder).map { it })

		with(flipper) {
			addNode(HORIZONTAL_P) { folderPerspective(folder, Orientation.HORIZONTAL, HORIZONTAL_P) }
			addNode(VERTICAL_P) { folderPerspective(folder, Orientation.VERTICAL, VERTICAL_P) }
		}

		return flipper
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

		//	when a new folder is activated switch to it -- only needs this for programmatic
		//  changes
		//model.folderSelectedEvent.subscribe { view.selectionModel.select(treeItems[it]) }

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

	private fun messageView(folder : IFolder, persp : String) : LazyFlipper<Boolean>
	{
		val messageStream = EventSource<IMessage>()
		val messageView = resizeable(MessageView(messageStream))

		val messageFlipper = SimpleFlipper()
		val loadSpinner = WaitSpinner("Loading Message")

		val noMessageSelected = boxIt(Label(Strings.NO_MESSAGE_SELECTED))

		setVgrow(noMessageSelected, Priority.ALWAYS)
		setVgrow(messageView, Priority.ALWAYS)

		val flipper = resizeable(LazyFlipper(model.sel(folder, persp).map { it.selectedIndices.size == 1 }))
		with(flipper) {
			addNode(true) { messageFlipper }
			addNode(false) { noMessageSelected }
		}

		setVgrow(flipper, Priority.ALWAYS)

		//	if the selection on the folder changes to a new single message display  it
		model.newM(folder, persp).subscribe {
			println("DISPLAY MESSAGE::" + it + "::" + it + "::" + it.subjectProperty.get())

			//	don't mark junk as read
			if (it.account.isDisplayMessageMarksAsRead && !it.folder.isSpecial(Vocabulary.JUNK_FOLDER))
			{
				it.folder.markMessagesAsRead(listOf(it))
			}

			//  start a sync on the message
			val loadMessage = it.sync()

			//  start load aheads around the message
			folder.syncAhead(model.selectionModels[folder]!!.selectedIndices,
				(model.selectionModels[folder]!! as TableView.TableViewSelectionModel).tableView.items)

			messageFlipper.flip(loadSpinner)

			//  start content loading -- single threaded executor should ensure good behaviour
			//  if we push a new message
			PinkPigMail.htmlState.message = it
			PinkPigMail.service.submit {
				println("STARTING CONTENT LOAD " + it.id)
				loadMessage.get()
				println("CONTENT LOADED PUSHING FOR " + it.id)
				later {
					messageStream.push(it)
					messageFlipper.flip(messageView)
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
//
//private fun displayMessage(folder : IFolder,
//                           message : IMessage,
//                           messageStream : EventSource<IMessage>,
//                           perspective : String)
//{
//	println("DISPLAY MESSAGE::" + perspective + "::" + message + "::" + message.subjectProperty.get())
//
//	//	don't mark junk as read
//	if (message.account.isDisplayMessageMarksAsRead && !message.folder.isSpecial(Vocabulary.JUNK_FOLDER))
//	{
//		message.folder.markMessagesAsRead(listOf(message))
//	}
//
//	//  start loadaheads on the message
//	val loadMessage = message.loadAhead()
//
//	//  start load aheads on the message frame
//	loadAhead(folder).forEach { message.folder.loadAhead(it) }
//
//	//  start content loading -- single threaded executor should ensure good behaviour
//	//  if we push a new message
//	PinkPigMail.htmlState.message = message
//	PinkPigMail.service.submit {
//		println("STARTING CONTENT LOAD " + message.id)
//		loadMessage.get()
//		println("CONTENT LOADED PUSHING FOR " + message.id)
//		later {
//			messageStream.push(message)
//		}
//	}
//}