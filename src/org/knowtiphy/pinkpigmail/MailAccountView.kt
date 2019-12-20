package org.knowtiphy.pinkpigmail

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
import org.knowtiphy.pinkpigmail.cell.*
import org.knowtiphy.pinkpigmail.cell.DateCell
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.Format
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

class MailAccountView(stage: Stage, account: IEmailAccount) : VBox()
{
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
					model.changePerspective(folder, "VERTICAL")

					//	loadahed should go here
//					viewModel.selection[folder]!!.subscribe { println("Selected " + it) }
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

		//  message load-ahead
		// accountViewModel.entitySelected.filter { it.getSelectionModel(folder).selectedIndices.isNotEmpty() }.subscribe { loadAhead(it) }

		//model.category.subscribe { categoryProperty.value = it.newValue }

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

	private fun loadAhead(folder: IFolder, total: List<IMessage>): List<IMessage>
	{
//		val pos = s(folder).selectedIndices[sm(folder).selectedIndices.size - 1]
//		//  TODO -- should do better than this, expand outwards, especially if we have a multi-selection
//		//  load ahead radially 4 messages either side of pos
//		val n = total.size
//		val result = ArrayList<IMessage>()
//		for (i in 1 until 5)
//		{
//			val before = pos - i
//			if (before in 0 until n)
//			{
//				result.add(total[before])
//			}
//			val after = pos + i
//			if (after in 0 until n)
//			{
//				result.add(total[after])
//			}
//		}
//
//		return result
		return ArrayList<IMessage>()
	}

	private fun displayMessage(folder: IFolder, messages: Collection<IMessage>, total: List<IMessage>,
							   messageProp: ObjectProperty<Pair<IMessage?, Collection<IMessage>>>)
	{
		if (messages.size == 1)
		{
			val message = messages.first()
			PinkPigMail.htmlState.message = message
			messageProp.set(Pair(message, loadAhead(folder, total)))
			//	don't mark junk as read
			if (message.mailAccount.isDisplayMessageMarksAsRead && !message.junkProperty.get())
			{
				message.folder.markMessagesAsRead(listOf(message))
			}
		}
	}

	//  the message list for one folder perspective

	private fun createFolderMessageList(folder: IFolder): TableView<IMessage>
	{
		val view = resizeable(TableView<IMessage>())

		val selModel = model.bindSelectionModel(folder, view.selectionModel)
		if (selModel != null)
			view.selectionModel = selModel

		val cellValueFactory = { param: TableColumn.CellDataFeatures<IMessage, IMessage> -> ReadOnlyObjectWrapper(param.value) }

		val statusColumn = TableColumn<IMessage, IMessage>()
		statusColumn.setCellValueFactory(cellValueFactory)
		statusColumn.setCellFactory { StatusCell() }
		with(statusColumn) {
			prefWidth = 35.0
			maxWidth = 35.0
			minWidth = 35.0
			isSortable = false
		}

		val fromColumn = TableColumn<IMessage, IMessage>(Strings.FROM)
		fromColumn.setCellValueFactory(cellValueFactory)
		with(fromColumn) {
			setCellFactory { AddressCell(model.account) { it.from } }
			prefWidth = 300.0
			comparator = UIUtils.cmp { it.from[0] }
		}

		val receivedCol = TableColumn<IMessage, IMessage>(Strings.RECEIVED)
		receivedCol.setCellValueFactory(cellValueFactory)
		with(receivedCol) {
			setCellFactory { DateCell { it.receivedOnProperty } }
			prefWidth = 200.0
			comparator = UIUtils.cmp { it.receivedOnProperty.get() }
			sortType = TableColumn.SortType.DESCENDING
		}

		val subjectCol = TableColumn<IMessage, IMessage>(Strings.SUBJECT)
		with(subjectCol) {
			setCellValueFactory(cellValueFactory)
			setCellFactory { GeneralCell<String, StringProperty>({ it.subjectProperty }, { Format.asDate(it) }) }
			remainingWidth()
			prefWidth = 750.0
			comparator = UIUtils.cmp { it.subjectProperty.get() }
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
				{ model.changePerspective(folder, if (orientation == Orientation.VERTICAL) "HORIZONTAL" else "VERTICAL") },
				if (orientation == Orientation.VERTICAL) Strings.SWITCH_HORIZONTAL else Strings.SWITCH_VERTICAL, false)
		val reply = action(Icons.reply(), { Actions.replyToMessage(sel(folder)) }, Strings.REPLY, true)
		val replyAll = action(Icons.replyAll(), { Actions.replyToMessage(sel(folder), true) }, Strings.REPLY_ALL, true)
		val forward = action(Icons.forward(), { Actions.forwardMail(sel(folder)) }, Strings.FORWARD, true)
		val delete = action(Icons.delete(), {
			//  move to the next message -- by default JavaFX goes back to the previous message
			val indices = model.currentSelection(folder).selectedIndices
			Actions.deleteMessages(sels(folder))
			model.tableViewSelectionModels[folder]!!.clearAndSelect(if (indices.isEmpty()) 0 else indices.last() + 1)
		}, Strings.DELETE, true)
		val markJunk = action(Icons.markJunk(),
				{
					//  move to the next message -- by default JavaFX goes back to the previous message
					val indices = model.currentSelection(folder).selectedIndices
					Actions.markMessagesAsJunk(sels(folder))
					model.tableViewSelectionModels[folder]!!.clearAndSelect(if (indices.isEmpty()) 0 else indices.last() + 1)
				}, Strings.MARK_JUNK, true)
		val markNotJunk = action(Icons.markNotJunk(), { Actions.markMessagesAsNotJunk(sels(folder)) }, Strings.MARK_NOT_JUNK, true)

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
		val messageProperty = SimpleObjectProperty<Pair<IMessage?, Collection<IMessage>>>()

		val messageView = resizeable(MessageView(PinkPigMail.service, messageProperty))
		val messageList = SplitPane(createFolderMessageList(folder))

		val splitPane = resizeable(SplitPane(messageList, messageView))
		splitPane.orientation = orientation

		val toolBar = maxSizeable(toolBar(folder, orientation, name))
		//  need this to make disabling of buttons show up
		toolBar.background = Background(BackgroundFill(Color.WHITE, null, null))

		setVgrow(toolBar, Priority.NEVER)
		setVgrow(splitPane, Priority.ALWAYS)

		//	if the selection on the folder changes display a new message
		model.selection[folder]!!.filter { isCP(folder, name) && it.size() == 1 }.subscribe { displayMessage(folder, it.selectedItems, ArrayList(), messageProperty) }

		return resizeable(VBox(toolBar, splitPane))
	}

	//  create a view of one folder -- a flipper of different folder perspectives

	private fun folderView(folder: IFolder): Node
	{
		val flipper = LazyMappedReplacer(model.perspective[folder]!!.map { it.newValue })
		flipper.addNode("HORIZONTAL") { folderPerspective(folder, Orientation.HORIZONTAL, "HORIZONTAL") }
		flipper.addNode("VERTICAL") { folderPerspective(folder, Orientation.VERTICAL, "VERTICAL") }
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