package org.knowtiphy.pinkpigmail.mailaccountview

import javafx.beans.binding.Bindings
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.collections.transformation.SortedList
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Stage
import org.knowtiphy.pinkpigmail.Actions
import org.knowtiphy.pinkpigmail.MessageView
import org.knowtiphy.pinkpigmail.PinkPigMail
import org.knowtiphy.pinkpigmail.cell.*
import org.knowtiphy.pinkpigmail.cell.DateCell
import org.knowtiphy.pinkpigmail.mailview.HTMLState
import org.knowtiphy.pinkpigmail.model.EmailAddress
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.resources.Beep
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.Format
import org.knowtiphy.pinkpigmail.util.Functions
import org.knowtiphy.pinkpigmail.util.ui.LazyMappedReplacer
import org.knowtiphy.pinkpigmail.util.ui.MappedReplacer
import org.knowtiphy.pinkpigmail.util.ui.UIUtils
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.action
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.button
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.later
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.maxSizeable
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.resizeable
import tornadofx.SmartResize
import tornadofx.remainingWidth
import java.time.ZonedDateTime
import java.util.concurrent.ExecutorService

class MailAccountView(stage: Stage, private val service: ExecutorService, private val htmlState: HTMLState, account: IEmailAccount) : VBox()
{
	companion object
	{
		private const val HORIZONTAL = "Horizontal"
		private const val VERTICAL = "Vertical"
	}

	private val model = AccountViewModel<IEmailAccount, IFolder, IMessage>(account)
	private val foldersRoot = TreeItem<IFolder>()
	private val treeItems = HashMap<IFolder, TreeItem<IFolder>>()

	//  the folder flipper that shows the current folder view to the right of the folder list
	private val folderViewFlipper = resizeable(MappedReplacer(model.currentCategoryProperty()))
	//  the folder space -- folder list on the left, folder view on right
	private val folderSpace = resizeable(SplitPane(createFoldersList(), folderViewFlipper))

	init
	{
		setVgrow(folderSpace, Priority.ALWAYS)
		try
		{
			folderSpace.setDividerPositions(PinkPigMail.uiSettings.verticalPosition[0].position)
		}
		catch (ex: IndexOutOfBoundsException)
		{
			//  ignore
		}
		children.addAll(folderSpace)

		//  when a folder is added to the account's folder list add an item to folder list on the left, and
		//  add a per folder view to the folder flipper

		account.folders.addListener { c: ListChangeListener.Change<out IFolder> ->
			while (c.next())
			{
				c.addedSubList.forEach { folder ->
					val treeItem = TreeItem<IFolder>(folder)
					treeItems[folder] = treeItem
					foldersRoot.children.add(treeItem)
					model.addCategory(folder)
					folderViewFlipper.addNode(folder, folderView(folder))
					//	TODO -- get the initial perspective from some sort of UI settings
					model.changePerspective(folder, VERTICAL)

					//  message load-ahead
					//model.selection[folder]!!.filter { !it.isEmpty() }.subscribe { loadAhead(folder, it) }

					//	loadahed should go here
					//  publish events -- new message selected
					//   fvm.entitySelected.subscribe { accountViewModel.getCategoryViewModel()!!.entitySelected.set(fvm) })
				}
			}
		}

		//  TODO -- this is necessary (is it still necessary?) to try to work around a JavaFX bug with setting split pane positions?
		stage.setOnShown {
			later {
				try
				{
					folderSpace.setDividerPositions(PinkPigMail.uiSettings.verticalPosition[0].position)
					Bindings.bindContent<SplitPane.Divider>(PinkPigMail.uiSettings.verticalPosition, folderSpace.dividers)
				}
				catch (ex: Exception)
				{
					//  ignore
				}
			}
		}

		//  on synch finished set the view to the inbox, beep if there are unread messages in the inbox, and
		//  set up beeping on any new messages arriving in the inbox
		PinkPigMail.synched.filter { it == account }.subscribe {
			model.changeCategory(model.account.inbox)
			if (model.account.inbox.unreadMessageCountProperty.get() > 0) Beep.beep()
			model.account.inbox.unreadMessageCountProperty.addListener { _: ObservableValue<out Number>, oldV: Number, newV: Number ->
				if (newV.toInt() > oldV.toInt()) Beep.beep()
			}
		}
	}

//	private fun loadAhead(folder: IFolder, selection: EntitySelection<IFolder, IMessage>)
//	{
//		val pos = selection.selectedIndices.first()
//		val underlyingMessages = model.currentTableViewSelectionModel(folder).tableView.items
//		val n = underlyingMessages.size
//		val range = 0 until n
//		for (i in 1 until 5)
//		{
//			val disti = ArrayList<IMessage>()
//			val after = pos + i
//			if (after in range)
//			{
//				disti.add(underlyingMessages[after])
//			}
//			val before = pos - i
//			if (before in range)
//			{
//				disti.add(underlyingMessages[before])
//			}
//
//			folder.loadAhead(disti)
//		}
//	}

	//  load ahead radially 4 messages either side of the selection

	private fun loadAhead(folder: IFolder, selection: EntitySelection<IFolder, IMessage>): List<List<IMessage>>
	{
		val pos = selection.selectedIndices.first()
		val underlyingMessages = model.currentTableViewSelectionModel(folder).tableView.items
		val n = underlyingMessages.size
		val result = ArrayList<ArrayList<IMessage>>()
		val range = 0 until n
		for (i in 1 until 5)
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

	private fun displayMessage(folder: IFolder, selection: EntitySelection<IFolder, IMessage>,
							   messageProp: ObjectProperty<Pair<IMessage?, Collection<Collection<IMessage>>>>)
	{
		if (selection.size() != 1)
		{
			messageProp.set(Pair(null, listOf()))
		} else
		{
			val message = selection.selectedItem()
			htmlState.message = message
			messageProp.set(Pair(message, loadAhead(folder, selection)))
			//	don't mark junk as read
			if (message.mailAccount.isDisplayMessageMarksAsRead && !message.junkProperty.get())
			{
				message.folder.markMessagesAsRead(listOf(message))
			}
		}
	}

	//  the message list for one folder perspective

	private fun folderMessageList(folder: IFolder): TableView<IMessage>
	{
		val view = resizeable(TableView<IMessage>())

		val selModel = model.bindSelectionModel(folder, view.selectionModel)
		if (selModel != null)
			view.selectionModel = selModel

		val cellValueFactory = { param: TableColumn.CellDataFeatures<IMessage, IMessage> -> ReadOnlyObjectWrapper(param.value) }

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

	//	helper methods

	private fun sel(folder: IFolder) = model.currentSelection(folder).selectedItem()
	private fun sels(folder: IFolder) = model.currentSelection(folder).selectedItems
	private fun isCP(folder: IFolder, name: String) = model.isCurrentPerspective(folder, name)

	//  create the tool bar at the top of the folder perspective

	private fun toolBar(folder: IFolder, orientation: Orientation, name: String): HBox
	{
		val layout = action(Icons.switchHorizontal(),
				{ model.changePerspective(folder, if (orientation == Orientation.VERTICAL) HORIZONTAL else VERTICAL) },
				if (orientation == Orientation.VERTICAL) Strings.SWITCH_HORIZONTAL else Strings.SWITCH_VERTICAL, false)
		val reply = action(Icons.reply(), { Actions.replyToMessage(sel(folder)) }, Strings.REPLY)
		val replyAll = action(Icons.replyAll(), { Actions.replyToMessage(sel(folder), true) }, Strings.REPLY_ALL)
		val forward = action(Icons.forward(), { Actions.forwardMail(sel(folder)) }, Strings.FORWARD)
		val delete = action(Icons.delete(), {
			//  move to the next message -- by default JavaFX goes back to the previous message
			val indices = model.currentSelection(folder).selectedIndices
			Actions.deleteMessages(sels(folder))
			model.currentTableViewSelectionModel(folder).clearAndSelect(if (indices.isEmpty()) 0 else indices.last() + 1)
		}, Strings.DELETE)
		val markJunk = action(Icons.markJunk(),
				{
					//  move to the next message -- by default JavaFX goes back to the previous message
					val indices = model.currentSelection(folder).selectedIndices
					Actions.markMessagesAsJunk(sels(folder))
					model.currentTableViewSelectionModel(folder).clearAndSelect(if (indices.isEmpty()) 0 else indices.last() + 1)
				}, Strings.MARK_JUNK)
		val markNotJunk = action(Icons.markNotJunk(), { Actions.markMessagesAsNotJunk(sels(folder)) }, Strings.MARK_NOT_JUNK)

		val replyGroup = HBox(2.0, button(reply), button(replyAll), button(forward))
		val markGroup = HBox(2.0, button(delete), button(markJunk), button(markNotJunk))
		val middleButtons = HBox(15.0, replyGroup, markGroup)

		val toolBar = HBox(UIUtils.hSpacer(), middleButtons, UIUtils.hSpacer(), button(layout))
		toolBar.padding = Insets(1.0, 0.0, 1.0, 0.0)

		listOf(reply, replyAll, forward).forEach { action ->
			model.selection[folder]!!.filter { isCP(it.category, name) }.subscribe {
				action.disabledProperty().value = it.size() != 1
			}
		}

		listOf(delete, markJunk, markNotJunk).forEach { action ->
			model.selection[folder]!!.filter { isCP(it.category, name) }.subscribe {
				action.disabledProperty().value = it.isEmpty()
			}
		}

		return toolBar
	}

	//  create a perspective of one folder -- layed out either horizontally or vertically

	private fun folderPerspective(folder: IFolder, orientation: Orientation, name: String): VBox
	{
		val messageProperty = SimpleObjectProperty<Pair<IMessage?, Collection<Collection<IMessage>>>>()

		val messageView = resizeable(MessageView(service, messageProperty))
		val messageList = SplitPane(folderMessageList(folder))

		val splitPane = resizeable(SplitPane(messageList, messageView))
		splitPane.orientation = orientation

		val toolBar = maxSizeable(toolBar(folder, orientation, name))
		//  need this to make disabling of buttons show up
		toolBar.background = Background(BackgroundFill(Color.WHITE, null, null))

		setVgrow(toolBar, Priority.NEVER)
		setVgrow(splitPane, Priority.ALWAYS)

		//	if the selection on the folder changes display a new message
		model.selection[folder]!!.filter { isCP(folder, name) }.subscribe { displayMessage(folder, it, messageProperty) }

		return resizeable(VBox(toolBar, splitPane))
	}

	//  create a view of one folder -- a flipper of different folder perspectives

	private fun folderView(folder: IFolder): Node
	{
		val flipper = LazyMappedReplacer(model.perspective[folder]!!.map { it.newValue })
		flipper.addNode(HORIZONTAL) { folderPerspective(folder, Orientation.HORIZONTAL, HORIZONTAL) }
		flipper.addNode(VERTICAL) { folderPerspective(folder, Orientation.VERTICAL, VERTICAL) }
		return flipper
	}

	//  the tree view of folders on the left and it's toolbar

	private fun createFoldersList(): VBox
	{
		val view = resizeable(TreeView(foldersRoot))
		with(view) {
			isShowRoot = false
			setCellFactory { FolderCell() }

			//  when a new folder is selected (single selection model) set the account view model category
			selectionModel.selectedItems.addListener { c: ListChangeListener.Change<out TreeItem<IFolder>> ->
				while (c.next())
				{
					if (c.wasAdded() && c.addedSize == 1)
					{
						model.changeCategory(c.addedSubList[0].value)
					}
				}
			}
		}

		model.category.subscribe { view.selectionModel.select(treeItems[it.newValue]) }

		val compose = action(Icons.compose(), { Actions.composeMail(model.account) }, Strings.COMPOSE, false)
//        val config = ActionHelper.create(Icons.configure(),
//                { Actions.configureAccount(pad.mailAccount) }, Strings.CONFIGURE_ACCOUNT, false)
		//val configButton = ButtonHelper.regular(config)

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