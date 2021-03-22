package org.knowtiphy.pinkpigmail.mailaccountview

import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleObjectProperty
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
import org.knowtiphy.pinkpigmail.model.events.FolderSyncDoneEvent
import org.knowtiphy.pinkpigmail.model.events.FolderSyncStartedEvent
import org.knowtiphy.pinkpigmail.model.events.MessageArrivedEvent
import org.knowtiphy.pinkpigmail.model.events.StageShowEvent
import org.knowtiphy.pinkpigmail.resources.Beep
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.Format
import org.knowtiphy.pinkpigmail.util.Functions
import org.knowtiphy.pinkpigmail.util.ui.LazyMappedReplacer
import org.knowtiphy.pinkpigmail.util.ui.UIUtils
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.action
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.boxIt
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.button
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.later
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.maxSizeable
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.resizeable
import org.reactfx.EventStreams
import tornadofx.SmartResize
import tornadofx.remainingWidth
import java.time.ZonedDateTime
import java.util.concurrent.ExecutorService

class MailAccountView(private val service: ExecutorService, account: IEmailAccount) : VBox()
{
	companion object
	{
		private const val HORIZONTAL_P = "Horizontal"
		private const val VERTICAL_P = "Vertical"
	}

	private val model = AccountViewModel<IEmailAccount, IFolder, IMessage>(account)
	private val foldersRoot = TreeItem<IFolder>()
	private val treeItems = HashMap<IFolder, TreeItem<IFolder>>()

	//  the folder flipper that shows the current folder view to the right of the folder list
	private val folderViewFlipper = resizeable(LazyMappedReplacer(model.folderSelectedEvent, 200.0))

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
		account.events
			.filter(MessageArrivedEvent::class.java)
			.filter { (it as MessageArrivedEvent).folder.isSpecial(Vocabulary.INBOX_FOLDER) }
			.subscribe { Beep.beep() }

		//	the voodoo below is to work around a JavaFX bug in setting split pane positions
		//	start with the folderList having a max width that is small
		//	then when the stage is shown change it to be resizable and set split pane positions
		folderList.maxWidth = 100.0
		PinkPigMail.fromModel.filter(StageShowEvent::class.java)
		later {
			maxSizeable(folderList)
			folderSpace.setDividerPositions(PinkPigMail.uiSettings.getAccountSettings(account).verticalPos.get())
			PinkPigMail.uiSettings.getAccountSettings(account).verticalPos.bind(folderSpace.dividers[0].positionProperty())
		}
	}

	private fun addFolder(folder: IFolder)
	{
		val treeItem = TreeItem<IFolder>(folder)
		treeItems[folder] = treeItem
		foldersRoot.children.add(treeItem)
		model.addCategory(folder)

		//	TODO -- this 0/1 stuff is a hack -- should use the actual event, but couldn't make it work
		//	with the typing

		//	TODO -- have to make the folderView for the change perspective to work
		val fView = folderView(folder)

		val syncView = LazyMappedReplacer(EventStreams.merge(
			folder.account.events.filter(FolderSyncStartedEvent::class.java).map { 0 },
			folder.account.events.filter(FolderSyncDoneEvent::class.java).map { 1 }
		))

		//	TODO -- use a spinner or something nicer than a big ass label
		syncView.addNode(0) { boxIt(Label("SYNCHING")) }
		syncView.addNode(1) { fView }

		folderViewFlipper.addNode(folder, syncView)
		//	TODO -- get the initial perspective from some sort of UI settings

		model.changePerspective(folder, VERTICAL_P)
		//  message load-ahead
		//model.selection[folder]!!.filter { !it.isEmpty() }.subscribe { loadAhead(folder, it) }
	}

	//  load ahead radially 3 messages either side of the selection
	//	this needs to be improved for when we have multiple selections

	private fun loadAhead(folder: IFolder): List<List<IMessage>>
	{
		val pos = model.selectionModels[folder]!!.selectedIndices.first()
		val underlyingMessages = folder.messages
		val n = underlyingMessages.size
		val result = ArrayList<ArrayList<IMessage>>()
		val range = 0 until n
		for (i in 1 until 4)
		{
			val disti = ArrayList<IMessage>()
			val after = pos + i
			if (after in range)
			{
				disti.add(underlyingMessages[after])
			}
			val before = pos - i
			if (before in range)
			{
				disti.add(underlyingMessages[before])
			}

			result.add(disti)
		}

		return result
	}

	private fun displayMessage(folder: IFolder, message: IMessage, messageProp: ObjectProperty<IMessage>)
	{
		PinkPigMail.htmlState.message = message
		message.loadAhead()
		model.loadAhead.addMatch(message, loadAhead(folder))
		messageProp.set(message)

		//	don't mark junk as read
		if (message.account.isDisplayMessageMarksAsRead && !message.folder.isJunkProperty.get())
		{
			message.folder.markMessagesAsRead(listOf(message))
		}
	}

	//  the message list for one folder perspective

	private fun messageList(folder: IFolder): TableView<IMessage>
	{
		val view = resizeable(TableView<IMessage>())

		//	see the comment in model.setSelection
		if (!model.selectionModels.containsKey(folder))
			model.setSelection(folder, view.selectionModel)
		else
			view.selectionModel = model.selectionModels[folder] as TableView.TableViewSelectionModel<IMessage>

		val cellValueFactory =
			{ param: TableColumn.CellDataFeatures<IMessage, IMessage> -> ReadOnlyObjectWrapper(param.value) }

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

		return view
	}

	//	helper methods for the buttons
	//	TODO -- they should really use events and store state in them but ah well

	private fun M(folder: IFolder) = model.selectionModels[folder]!!
	private fun MI(folder: IFolder) = M(folder).selectedItem
	private fun MIS(folder: IFolder) = M(folder).selectedItems
	private fun isCP(folder: IFolder, name: String) = model.isCurrentPerspective(folder, name)

	//  create the tool bar at the top of the folder perspective

	private fun toolBar(folder: IFolder, orientation: Orientation, name: String): HBox
	{
		val layout = action(
			Icons.switchHorizontal(),
			{
				model.changePerspective(
					folder,
					if (orientation == Orientation.VERTICAL) HORIZONTAL_P else VERTICAL_P
				)
			},
			if (orientation == Orientation.VERTICAL) Strings.SWITCH_HORIZONTAL else Strings.SWITCH_VERTICAL, false
		)

		val reply = action(Icons.reply(), { Actions.replyToMessage(MI(folder)) }, Strings.REPLY)
		val replyAll = action(Icons.replyAll(), { Actions.replyToMessage(MI(folder), true) }, Strings.REPLY_ALL)
		val forward = action(Icons.forward(), { Actions.forwardMail(MI(folder)) }, Strings.FORWARD)
		val delete = action(Icons.delete(), {
			//  move to the next message -- by default JavaFX goes back to the previous message
			Actions.deleteMessages(MIS(folder))
			M(folder).selectNext()
		}, Strings.DELETE)
		val markJunk = action(
			Icons.markJunk(), {
				//  move to the next message -- by default JavaFX goes back to the previous message
				Actions.markMessagesAsJunk(MIS(folder))
				M(folder).selectNext()
			}, Strings.MARK_JUNK
		)
		val markNotJunk = action(
			Icons.markNotJunk(), { Actions.markMessagesAsNotJunk(MIS(folder)) },
			Strings.MARK_NOT_JUNK
		)

		val replyGroup = HBox(2.0, button(reply), button(replyAll), button(forward))
		val markGroup = HBox(2.0, button(delete), button(markJunk), button(markNotJunk))
		val middleButtons = HBox(15.0, replyGroup, markGroup)

		val toolBar = HBox(UIUtils.hSpacer(), middleButtons, UIUtils.hSpacer(), button(layout))
		toolBar.padding = Insets(1.0, 0.0, 1.0, 0.0)

		listOf(reply, replyAll, forward).forEach { action ->
			model.selection[folder]!!.filter { isCP(folder, name) }
				.subscribe { action.disabledProperty().value = it.selectedItems.size != 1 }
		}

		listOf(delete, markJunk, markNotJunk).forEach { action ->
			model.selection[folder]!!.subscribe { action.disabledProperty().value = it.isEmpty }
		}

		return toolBar
	}

	//  create a vertical/horizontal perspective of one folder

	private fun folderPerspective(folder: IFolder, orientation: Orientation, name: String): VBox
	{
		val messageProperty = SimpleObjectProperty<IMessage>()

		val messageView = resizeable(MessageView(model.account, service, messageProperty))
		val messageList = SplitPane(messageList(folder))

		val splitPane = resizeable(SplitPane(messageList, messageView))
		splitPane.orientation = orientation

		val toolBar = maxSizeable(toolBar(folder, orientation, name))
		//  need this to make disabling of buttons show up
		toolBar.background = Background(BackgroundFill(Color.WHITE, null, null))

		setVgrow(toolBar, Priority.NEVER)
		setVgrow(splitPane, Priority.ALWAYS)

		//	if the selection changes to multiple clear the message display
		model.selection[folder]!!.filter { it.selectedIndices.size != 1 }.subscribe { messageProperty.set(null) }

		//	if the selection on the folder changes to a new single message display  it
		model.selection[folder]!!.filter { it.selectedIndices.size == 1 }.subscribe {
			displayMessage(folder, it.selectedItem, messageProperty)
		}

		return resizeable(VBox(toolBar, splitPane))
	}

	//  create a view of one folder -- a flipper of different folder perspectives

	private fun folderView(folder: IFolder): Node
	{
		val flipper = LazyMappedReplacer(model.perspective[folder]!!.map { it }, 1000.0)
		flipper.addNode(HORIZONTAL_P) {
			folderPerspective(folder, Orientation.HORIZONTAL, HORIZONTAL_P)
		}
		flipper.addNode(VERTICAL_P) {
			folderPerspective(folder, Orientation.VERTICAL, VERTICAL_P)
		}
		return flipper
	}

//  the tree view of folders on the left and it's toolbar

	private fun createFoldersList(): VBox
	{
		val view = resizeable(TreeView(foldersRoot))
		with(view) {
			isShowRoot = false
			setCellFactory { FolderCell() }

			//  when a folder is selected (single selection model) set the account view model category
			EventStreams.changesOf(selectionModel.selectedItems).subscribe {
				while (it.next())
				{
					if (it.wasAdded() && it.addedSize == 1)
					{
						model.changeFolder(it.addedSubList[0].value)
					}
				}
			}
		}

		//	when a new folder is activated switch to it
		model.folderSelectedEvent.subscribe { view.selectionModel.select(treeItems[it]) }

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