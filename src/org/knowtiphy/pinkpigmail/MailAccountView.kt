package org.knowtiphy.pinkpigmail

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.collections.ListChangeListener
import javafx.collections.transformation.SortedList
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.knowtiphy.pinkpigmail.cell.*
import org.knowtiphy.pinkpigmail.cell.DateCell
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.*
import org.reactfx.EventStreams
import tornadofx.SmartResize
import tornadofx.remainingWidth

class MailAccountView(stage: Stage, account: IEmailAccount) : VBox()
{
    private val accountViewModel = AccountViewModel<IEmailAccount, IFolder, IMessage>(account)
    private val foldersRoot = TreeItem<IFolder>()

    init
    {
        val foldersList = createFoldersList()

        //  the folder flipper that shows the current folder view to the right of the folder list
        val folderViewFlipper = MappedFlipper<IFolder>(accountViewModel.currentEntityProperty)
        UIUtils.resizable(folderViewFlipper)

        //  when a folder is added to the account's folder list add an item to folder list on the left, and
        //  add a per folder view to the folder flipper

        account.folders.addListener { c: ListChangeListener.Change<out IFolder> ->
            while (c.next())
            {
                c.addedSubList.forEach { folder ->
                    val fvm = CategoryViewModel<IFolder, IMessage>(folder)

                    accountViewModel.addCategoryViewModel(folder, fvm)
                    val treeItem = TreeItem<IFolder>(folder)
                    foldersRoot.children.add(treeItem)

                    val folderView = createFolderView(fvm)

                    //  publish events -- new message selected
                    EventStreams.changesOf((fvm.selectionModel
                            ?: return@forEach).selectedIndices).subscribe { accountViewModel.entitySelected.push(fvm) }

                    //  event streams for the folder becoming an inbox
                    val inBoxSet = EventStreams.changesOf(folder.isInboxProperty)

                    //  event stream for the folder increasing its unread message count
                    val unreadCountChanged = EventStreams.changesOf(folder.unreadMessageCountProperty)
                            .filter { c -> c.newValue.toInt() > c.oldValue.toInt() }

                    //  if the folder becomes an inbox select it
                    inBoxSet.subscribe { if (it.newValue) foldersList.selectionModel.select(treeItem) }
                    //  if the folder becomes and inbox and has unread messages, beep
                    EventStreams.combine(inBoxSet, unreadCountChanged).subscribe { println("GOT UNREAD MAIL "); PinkPigMail.beep(); }

                    folderViewFlipper.addNode(folder, folderView)
                }
            }
        }

        val toolBar = createToolBar()
        setVgrow(toolBar, Priority.NEVER)

        //  the folder space below the tool bar
        val folderSpace = SplitPane(foldersList, folderViewFlipper)
        setVgrow(folderSpace, Priority.ALWAYS)
        UIUtils.resizable(folderSpace)
        folderSpace.setDividerPositions(PinkPigMail.uiSettings.verticalPosition[0].position)

        //  TODO is this is necessary to try to work around a JavaFX bug with setting split pane positions?
        stage.setOnShown {
            Platform.runLater {
                folderSpace.setDividerPositions(PinkPigMail.uiSettings.verticalPosition[0].position)
                Bindings.bindContent<SplitPane.Divider>(PinkPigMail.uiSettings.verticalPosition, folderSpace.dividers)
            }
        }

        //  message load-ahead
        accountViewModel.entitySelected.filter { it.selectionModel!!.selectedIndices.isNotEmpty() }.subscribe { loadAhead(it) }

        children.addAll(toolBar, folderSpace)
    }

    private fun loadAhead(fvm: CategoryViewModel<IFolder, IMessage>)
    {
        val model = fvm.selectionModel as TableView.TableViewSelectionModel<IMessage>
        assert(model.selectedItems.isNotEmpty())

        val pos = model.selectedIndices[model.selectedIndices.size - 1]
        //  TODO -- should do better than this, expand outwards, especially if we have a multi-selection
        //  load ahead radially 4 messages either side of pos
        val n = model.tableView.items.size
        println("Starting loadAhead")
        for (i in 1 until 5)
        {
            val before = pos - i
            if (before in 0 until n)
            {
                model.tableView.items[before].ensureContentLoaded(false)
            }
            val after = pos + i
            if (after in 0 until n)
            {
                model.tableView.items[after].ensureContentLoaded(false)
            }
        }
    }

    private fun displayMessage(messages: List<IMessage>, messageView: MessageView)
    {
        if (messages.size == 1)
        {
            val message = messages[messages.size - 1]
            PinkPigMail.htmlState.message = message
            messageView.setMessage(message)
            //	don't mark junk as read
            if (message.mailAccount.isDisplayMessageMarksAsRead && !message.junkProperty.get())
            {
                message.folder.markMessagesAsRead(listOf(message))
            }
        }
    }

    //  create the tool bar at the top of the view
    private fun createToolBar(): HBox
    {
//        val config = ActionHelper.create(Icons.configure(),
//                { Actions.configureAccount(pad.mailAccount) }, Strings.CONFIGURE_ACCOUNT, false)
//      val layout = ActionHelper.create(Icons.switchHorizontal(),
//                {
//                    val p = pad.currentFolderViewModel().visiblePerspective
//                    p.set(if (p.get() == FolderSettings.HORIZONTAL_VIEW) FolderSettings.VERTICAL_VIEW else FolderSettings.HORIZONTAL_VIEW)
//                }, Strings.SWITCH_HORIZONTAL, false)
        val reply = ActionHelper.create(Icons.reply(), { Actions.replyToMessage(accountViewModel.currentEntity(), false) }, Strings.REPLY)
        val replyAll = ActionHelper.create(Icons.replyAll(), { Actions.replyToMessage(accountViewModel.currentEntity(), true) }, Strings.REPLY_ALL)
        val forward = ActionHelper.create(Icons.forward(), { Actions.forwardMail(accountViewModel.currentEntity()) }, Strings.FORWARD)
        val compose = ActionHelper.create(Icons.compose(), { Actions.composeMail(accountViewModel.account) }, Strings.COMPOSE, false)
        val delete = ActionHelper.create(Icons.delete(),
                {
                    //  move to the next message
                    val indices = accountViewModel.currentCategoryViewModel().selectionModel!!.selectedIndices
                    Actions.deleteMessages(accountViewModel.currentEntities())
                    accountViewModel.currentCategoryViewModel().selectionModel!!.clearAndSelect(if (indices.isEmpty()) 0 else indices[indices.size - 1] + 1)
                }, Strings.DELETE)
        val markJunk = ActionHelper.create(Icons.markJunk(),
                {
                    //  move to the next message
                    val indices = accountViewModel.currentCategoryViewModel().selectionModel!!.selectedIndices
                    Actions.markMessagesAsJunk(accountViewModel.currentEntities())
                    accountViewModel.currentCategoryViewModel().selectionModel!!.clearAndSelect(if (indices.isEmpty()) 0 else indices[indices.size - 1] + 1)
                }, Strings.MARK_JUNK)
        val markNotJunk = ActionHelper.create(Icons.markNotJunk(),
                { Actions.markMessagesAsNotJunk(accountViewModel.currentEntities()) }, Strings.MARK_NOT_JUNK)

        val singleMessageActions = arrayOf(reply, replyAll, forward)
        val multiMessageActions = arrayOf(delete, markJunk, markNotJunk)

        val replyGroup = HBox(ButtonHelper.regular(reply), ButtonHelper.regular(replyAll), ButtonHelper.regular(forward))
        replyGroup.spacing = 2.0
        val markGroup = HBox(ButtonHelper.regular(delete), ButtonHelper.regular(markJunk), ButtonHelper.regular(markNotJunk))
        markGroup.spacing = 2.0
        val middleButtons = HBox(ButtonHelper.regular(compose), replyGroup, markGroup)
        middleButtons.spacing = 15.0

        //val configButton = ButtonHelper.regular(config)
//        val layoutButton = ButtonHelper.regular(layout)

        val toolBar = HBox()
        toolBar.children.addAll(UIUtils.spacer(), middleButtons, UIUtils.spacer())
        toolBar.padding = Insets(1.0, 0.0, 1.0, 0.0)
        toolBar.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)

        accountViewModel.entitySelected.subscribe { fvm ->
            singleMessageActions.forEach { it.disabledProperty().set(fvm.selectionModel!!.selectedIndices.size != 1) }
            multiMessageActions.forEach { it.disabledProperty().set(fvm.selectionModel!!.selectedIndices.isEmpty()) }
        }

        return toolBar
    }

    //  the message list for one folder perspective
    private fun createFolderMessageList(fvm: CategoryViewModel<IFolder, IMessage>, messageView: MessageView): TableView<IMessage>
    {
        val view = TableView<IMessage>()
        UIUtils.resizable(view)

        view.columnResizePolicy = SmartResize.POLICY

        if (fvm.selectionModel == null)
        {
            view.selectionModel.selectionMode = SelectionMode.MULTIPLE
            fvm.selectionModel = view.selectionModel
        } else
        {
            view.selectionModel = fvm.selectionModel as TableView.TableViewSelectionModel<IMessage>
        }

        view.selectionModel.selectedItems.addListener { c: ListChangeListener.Change<out IMessage> ->
            while (c.next())
            {
                if (c.wasAdded() && c.addedSize != 0)
                {
                    displayMessage(c.list, messageView)
                }
            }
        }

        view.selectionModel.selectedCells.addListener { c: ListChangeListener.Change<out TablePosition<*, *>> ->
            while (c.next())
            {
                //  happens when we have selected the first row, and the first row only, and delete it
                if (!c.wasAdded() && !c.wasPermutated() && !c.wasRemoved() && !c.wasReplaced() && !c.wasUpdated())
                {
                    println("THE WIERD CASE IS HAPPENNING")
                    //  TODO -- do we need to simply publish a selection?
                    //displayMessage(headersView.selectionModel.selectedItems, messageView)
                }
            }
        }

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
            setCellFactory { AddressCell(fvm.category.accountProperty.get()) { it.from } }
            prefWidth = 300.0
            //  TODO
            comparator = Comparators.cmp { it.from[0] }
        }

        val receivedCol = TableColumn<IMessage, IMessage>(Strings.RECEIVED)
        receivedCol.setCellValueFactory(cellValueFactory)
        with(receivedCol) {
            setCellFactory { DateCell { it.receivedOnProperty } }
            prefWidth = 200.0
            comparator = Comparators.cmp { it.receivedOnProperty.get() }
            sortType = TableColumn.SortType.DESCENDING
        }

        val subjectCol = TableColumn<IMessage, IMessage>(Strings.SUBJECT)
        with(subjectCol) {
            setCellValueFactory(cellValueFactory)
            setCellFactory { GeneralCell({ it.subjectProperty }, { Format.format(it) }) }
            remainingWidth()
            prefWidth = 750.0
            comparator = Comparators.cmp { it.subjectProperty.get() }
        }

        view.columns.addAll(statusColumn, fromColumn, subjectCol, receivedCol)

        // sort on date received
        view.sortOrder.add(receivedCol)
        val sortedList = SortedList(fvm.category.messages)
        sortedList.comparatorProperty().bind(view.comparatorProperty())
        view.items = sortedList

        return view
    }

    //  create a perspective of one folder
    private fun createFolderPerspective(fvm: CategoryViewModel<IFolder, IMessage>, @Suppress("SameParameterValue") orientation: Orientation): SplitPane
    {
        val messageView = MessageView(PinkPigMail.service)
        UIUtils.resizable(messageView)
        val splitPane = SplitPane(createFolderMessageList(fvm, messageView), messageView)
        splitPane.orientation = orientation
        UIUtils.resizable(splitPane)
        return splitPane
    }

    //  create a view of one folder -- a flipper of different folder perspectives
    private fun createFolderView(fvm: CategoryViewModel<IFolder, IMessage>): SplitPane
    {
        val fSettings = PinkPigMail.uiSettings.getFolderSettings(fvm.category)

        val hPerspective = createFolderPerspective(fvm, Orientation.VERTICAL)
        //  val vPerspective = createFolderPerspective(fvm, folder, Orientation.HORIZONTAL)

        //  this is horrible
        hPerspective.dividers[0].position = fSettings.horizontalPositions[0].position
        // vPerspective.dividers[0].position = fSettings.verticalPositions[0].position

        Bindings.bindContent<SplitPane.Divider>(fSettings.horizontalPositions, hPerspective.dividers)
        //  Bindings.bindContent<SplitPane.Divider>(fSettings.verticalPositions, vPerspective.dividers)

//        val flipper = MappedFlipper<String>(fvm.visiblePerspective)
//        UIUtils.resizable(flipper)
//
//                // flipper.addNode(FolderSettings.HORIZONTAL_VIEW, hPerspective)
//        flipper.children.add(hPerspective)
//        //flipper.addNode(FolderSettings.VERTICAL_VIEW, vPerspective)
//        //  set the default view type
////        fvm.visiblePerspective.set(fSettings.viewType)

        return hPerspective
    }

    //  the tree view of folders on the left
    private fun createFoldersList(): TreeView<IFolder>
    {
        val view = TreeView(foldersRoot)
        UIUtils.resizable(view)
        view.isShowRoot = false
        view.setCellFactory { FolderCell() }

        //  publish events -- new folder selected (single selection model)
        EventStreams.changesOf(view.selectionModel.selectedItems)
                .filter { it.list.size == 1 }
                .subscribe { accountViewModel.currentEntityProperty.set(it.list[0].value) }

        return view
    }
}