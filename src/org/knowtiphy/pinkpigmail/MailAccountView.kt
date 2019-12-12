package org.knowtiphy.pinkpigmail

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
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
import org.knowtiphy.pinkpigmail.util.ActionHelper
import org.knowtiphy.pinkpigmail.util.Format
import org.knowtiphy.pinkpigmail.util.ui.ButtonHelper
import org.knowtiphy.pinkpigmail.util.ui.MappedReplacer
import org.knowtiphy.pinkpigmail.util.ui.UIUtils
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.maxSizeable
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.resizeable
import org.reactfx.EventStreams
import tornadofx.SmartResize
import tornadofx.remainingWidth

class MailAccountView(stage: Stage, account: IEmailAccount) : VBox()
{
    private val accountViewModel = AccountViewModel<IEmailAccount, IFolder, IMessage>(account)
    private val foldersRoot = TreeItem<IFolder>()

    init
    {
        val folderList = createFoldersList()
        val foo = SimpleObjectProperty<TreeItem<IFolder>>()
        //  the folder flipper that shows the current folder view to the right of the folder list
        val folderViewFlipper = resizeable(MappedReplacer<TreeItem<IFolder>>(foo))
        accountViewModel.categorySelected.subscribe { foo.set(it.newValue) }

        //  when a folder is added to the account's folder list add an item to folder list on the left, and
        //  add a per folder view to the folder flipper

        account.folders.addListener { c: ListChangeListener.Change<out IFolder> ->
            while (c.next())
            {
                c.addedSubList.forEach { folder ->
                    //val fvm = CategoryViewModel<IFolder, IMessage>(folder)

                    val treeItem = TreeItem<IFolder>(folder)
                    foldersRoot.children.add(treeItem)

                    val folderView = createFolderView(treeItem)
                    folderViewFlipper.addNode(treeItem, folderView)
                    println(folderViewFlipper.nodes)

                    //  publish events -- new message selected
                    //   fvm.entitySelected.subscribe { accountViewModel.getCategoryViewModel()!!.entitySelected.set(fvm) })

                    //  event streams for the folder becoming an inbox
                    val inBoxSet = EventStreams.changesOf(folder.isInboxProperty)

                    //  event stream for the folder increasing its unread message count
                    val unreadCountChanged = EventStreams.changesOf(folder.unreadMessageCountProperty)
                            .filter { c -> c.newValue.toInt() > c.oldValue.toInt() }

                    if (folder.isInboxProperty.get())
                    {
                        println("setting category for " + ((folder as IFolder).nameProperty.get()))
                        accountViewModel.setCategory(treeItem)
                    }

                    //  if the folder becomes an inbox select it
                    inBoxSet.subscribe { if (it.newValue) accountViewModel.setCategory(treeItem) }
                    //  if the folder becomes and inbox and has unread messages, beep
                    EventStreams.combine(inBoxSet, unreadCountChanged).subscribe { println("GOT UNREAD MAIL "); PinkPigMail.beep(); }
                }
            }
        }

        //  the folder space below the tool bar
        val folderSpace = resizeable(SplitPane(folderList, folderViewFlipper))
        setVgrow(folderSpace, Priority.ALWAYS)
        folderSpace.setDividerPositions(PinkPigMail.uiSettings.verticalPosition[0].position)

        //  TODO is this is necessary to try to work around a JavaFX bug with setting split pane positions?
        stage.setOnShown {
            Platform.runLater {
                folderSpace.setDividerPositions(PinkPigMail.uiSettings.verticalPosition[0].position)
                Bindings.bindContent<SplitPane.Divider>(PinkPigMail.uiSettings.verticalPosition, folderSpace.dividers)
            }
        }

        //  message load-ahead
        // accountViewModel.entitySelected.filter { it.getSelectionModel(folder).selectedIndices.isNotEmpty() }.subscribe { loadAhead(it) }

        children.addAll(folderSpace)
    }

    private fun loadAhead(folder: IFolder, total: List<IMessage>): List<IMessage>
    {
        val pos = accountViewModel.getSelectionModel(folder).selectedIndices[accountViewModel.getSelectionModel(folder).selectedIndices.size - 1]
        //  TODO -- should do better than this, expand outwards, especially if we have a multi-selection
        //  load ahead radially 4 messages either side of pos
        val n = total.size
        val result = ArrayList<IMessage>()
        for (i in 1 until 5)
        {
            val before = pos - i
            if (before in 0 until n)
            {
                result.add(total[before])
            }
            val after = pos + i
            if (after in 0 until n)
            {
                result.add(total[after])
            }
        }

        return result
    }

    private fun displayMessage(folder: IFolder, messages: List<IMessage>, total: List<IMessage>, messageView: MessageView)
    {
        if (messages.size == 1)
        {
            val message = messages[messages.size - 1]
            PinkPigMail.htmlState.message = message
            messageView.setMessage(Pair(message, loadAhead(folder, total)))
            //	don't mark junk as read
            if (message.mailAccount.isDisplayMessageMarksAsRead && !message.junkProperty.get())
            {
                message.folder.markMessagesAsRead(listOf(message))
            }
        }
    }

    //  create the tool bar at the top of the view
    private fun createToolBar(folder: IFolder): HBox
    {
//        val config = ActionHelper.create(Icons.configure(),
//                { Actions.configureAccount(pad.mailAccount) }, Strings.CONFIGURE_ACCOUNT, false)
//      val layout = ActionHelper.create(Icons.switchHorizontal(),
//                {
//                    val p = pad.currentFolderViewModel().visiblePerspective
//                    p.set(if (p.get() == FolderSettings.HORIZONTAL_VIEW) FolderSettings.VERTICAL_VIEW else FolderSettings.HORIZONTAL_VIEW)
//                }, Strings.SWITCH_HORIZONTAL, false)
        val reply = ActionHelper.create(Icons.reply(), { Actions.replyToMessage(accountViewModel.getSelectionModel(folder).selectedItem, false) }, Strings.REPLY)
        val replyAll = ActionHelper.create(Icons.replyAll(), { Actions.replyToMessage(accountViewModel.getSelectionModel(folder).selectedItem, true) }, Strings.REPLY_ALL)
        val forward = ActionHelper.create(Icons.forward(), { Actions.forwardMail(accountViewModel.getSelectionModel(folder).selectedItem) }, Strings.FORWARD)
        val delete = ActionHelper.create(Icons.delete(),
                {
                    //  move to the next message
                    val indices = accountViewModel.getSelectionModel(folder).selectedIndices
                    Actions.deleteMessages(accountViewModel.getSelectionModel(folder).selectedItems)
                    accountViewModel.getSelectionModel(folder).clearAndSelect(if (indices.isEmpty()) 0 else indices[indices.size - 1] + 1)
                }, Strings.DELETE)
        val markJunk = ActionHelper.create(Icons.markJunk(),
                {
                    //  move to the next message
                    val indices = accountViewModel.getSelectionModel(folder).selectedIndices
                    Actions.markMessagesAsJunk(accountViewModel.getSelectionModel(folder).selectedItems)
                    accountViewModel.getSelectionModel(folder).clearAndSelect(if (indices.isEmpty()) 0 else indices[indices.size - 1] + 1)
                }, Strings.MARK_JUNK)
        val markNotJunk = ActionHelper.create(Icons.markNotJunk(),
                { Actions.markMessagesAsNotJunk(accountViewModel.getSelectionModel(folder).selectedItems) }, Strings.MARK_NOT_JUNK)

        val singleMessageActions = arrayOf(reply, replyAll, forward)
        val multiMessageActions = arrayOf(delete, markJunk, markNotJunk)

        val replyGroup = HBox(ButtonHelper.regular(reply), ButtonHelper.regular(replyAll), ButtonHelper.regular(forward))
        replyGroup.spacing = 2.0
        val markGroup = HBox(ButtonHelper.regular(delete), ButtonHelper.regular(markJunk), ButtonHelper.regular(markNotJunk))
        markGroup.spacing = 2.0
        val middleButtons = HBox(replyGroup, markGroup)
        middleButtons.spacing = 15.0

        //val configButton = ButtonHelper.regular(config)
//        val layoutButton = ButtonHelper.regular(layout)

        val toolBar = HBox()
        toolBar.children.addAll(UIUtils.hSpacer(), middleButtons, UIUtils.hSpacer())
        toolBar.padding = Insets(1.0, 0.0, 1.0, 0.0)

        accountViewModel.getSelectionModel(folder).selectedIndices.addListener { c: ListChangeListener.Change<out Int> ->
            println("SET BUTTONS")
            while (c.next())
            {
                singleMessageActions.forEach { it.disabledProperty().set(c.list.size != 1) }
                multiMessageActions.forEach { it.disabledProperty().set(c.list.isEmpty()) }
            }
        }

        return toolBar
    }

    //  the message list for one folder perspective
    private fun createFolderMessageList(folder: IFolder, messageView: MessageView): TableView<IMessage>
    {
        val view = resizeable(TableView<IMessage>())
        view.selectionModel.selectionMode = SelectionMode.MULTIPLE

        view.columnResizePolicy = SmartResize.POLICY

        if (accountViewModel.isNullSelectionModel(folder))
        {
            accountViewModel.setSelectionModel(folder, view.selectionModel)
        } else
        {
            view.setSelectionModel(accountViewModel.getSelectionModel(folder) as TableView.TableViewSelectionModel<IMessage>)
        }

        // accountViewModel.addCategory(folder)

        view.selectionModel.selectedItems.addListener { c: ListChangeListener.Change<out IMessage> ->
            while (c.next())
            {
                if (c.wasAdded() && c.addedSize != 0)
                {
                    displayMessage(folder, c.list, view.items, messageView)
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
            setCellFactory { AddressCell(accountViewModel.account) { it.from } }
            prefWidth = 300.0
            //  TODO
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

        view.columns.addAll(statusColumn, fromColumn, subjectCol, receivedCol)

        // sort on date received
        view.sortOrder.add(receivedCol)
        val sortedList = SortedList(folder.messages)
        sortedList.comparatorProperty().bind(view.comparatorProperty())
        view.items = sortedList

        return view
    }

    //  create a perspective of one folder
    private fun createFolderPerspective(folder: TreeItem<IFolder>, @Suppress("SameParameterValue") orientation: Orientation): SplitPane
    {
        val messageView = resizeable(MessageView(PinkPigMail.service))
        val splitPane = resizeable(SplitPane(createFolderMessageList(folder.value, messageView), messageView))
        splitPane.orientation = orientation
        return splitPane
    }

    //  create a view of one folder -- a flipper of different folder perspectives
    private fun createFolderView(folder: TreeItem<IFolder>): VBox
    {
        val fSettings = PinkPigMail.uiSettings.getFolderSettings(folder.value)

        val hPerspective = createFolderPerspective(folder, Orientation.VERTICAL)
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

        val toolBar = maxSizeable(createToolBar(folder.value))
        setVgrow(toolBar, Priority.NEVER)

        setVgrow(hPerspective, Priority.ALWAYS)

        val box = resizeable(VBox())
        box.children.addAll(toolBar, hPerspective)

        return box
    }

    //  the tree view of folders on the left
    private fun createFoldersList(): VBox
    {
        val view = resizeable(TreeView(foldersRoot))
        view.isShowRoot = false
        view.setCellFactory { FolderCell() }

        //  new folder selected (single selection model)
        view.selectionModel.selectedItems.addListener { c: ListChangeListener.Change<out TreeItem<IFolder>> ->
            while (c.next())
            {
                if (c.wasAdded() && c.addedSize == 1)
                {
                    println("Selection changed to " + ((c.addedSubList[0].value as IFolder).nameProperty.get()))
                    accountViewModel.setCategory(c.addedSubList[0])
                }
            }
        }

        accountViewModel.categorySelected.subscribe {
            println("Setting view selection model to " + ((it.newValue.value) as IFolder).nameProperty.get())
            view.selectionModel.select(it.newValue)
        }

        val box = resizeable(VBox())

        val compose = ActionHelper.create(Icons.compose(), { Actions.composeMail(accountViewModel.account) }, Strings.COMPOSE, false)

        val toolBar = maxSizeable(HBox())
        toolBar.children.addAll(ButtonHelper.regular(compose))
        toolBar.padding = Insets(1.0, 0.0, 1.0, 0.0)

        setVgrow(toolBar, Priority.NEVER)
        setVgrow(view, Priority.ALWAYS)

        box.children.addAll(toolBar, view)

        return box
    }
}