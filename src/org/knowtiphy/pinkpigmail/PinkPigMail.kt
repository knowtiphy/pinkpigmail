package org.knowtiphy.pinkpigmail

import com.calendarfx.view.CalendarView
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener.Change
import javafx.collections.ObservableList
import javafx.collections.transformation.SortedList
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.TableColumn.CellDataFeatures
import javafx.scene.image.Image
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.stage.WindowEvent
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.controlsfx.glyphfont.Glyph
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.IStorageListener
import org.knowtiphy.babbage.storage.StorageFactory
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.Peer
import org.knowtiphy.pinkpigmail.cell.*
import org.knowtiphy.pinkpigmail.cell.DateCell
import org.knowtiphy.pinkpigmail.mailview.CustomURLStreamHandlerFactory
import org.knowtiphy.pinkpigmail.mailview.HTMLState
import org.knowtiphy.pinkpigmail.model.*
import org.knowtiphy.pinkpigmail.model.caldav.CalDAVAccount
import org.knowtiphy.pinkpigmail.model.caldav.CalDAVCalendar
import org.knowtiphy.pinkpigmail.model.caldav.CalDAVEvent
import org.knowtiphy.pinkpigmail.model.imap.IMAPAccount
import org.knowtiphy.pinkpigmail.model.imap.IMAPFolder
import org.knowtiphy.pinkpigmail.model.imap.IMAPMessage
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.*
import org.knowtiphy.utils.NameSource
import org.knowtiphy.utils.OS
import org.reactfx.EventStreams
import tornadofx.SmartResize
import tornadofx.remainingWidth
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.LogManager
import kotlin.system.exitProcess

/**
 * @author graham
 */

class PinkPigMail : Application(), IStorageListener
{
    init
    {
        val stream = PinkPigMail::class.java.getResourceAsStream("logging.properties")
        try
        {
            LogManager.getLogManager().readConfiguration(stream)
        } catch (ex: IOException)
        {
            ex.printStackTrace()
        }
    }

    companion object
    {
        private const val MESSAGE_STORAGE = "messages"

        //val LOGGER = Logger.getLogger(IMAPAdapter::class.java.name)

        const val STYLE_SHEET = "styles.css"

        val accounts: ObservableList<IAccount> = FXCollections.observableArrayList()

        val storage: IStorage by lazy {
            val dir = Paths.get(OS.getAppDir(PinkPigMail::class.java).toString(), MESSAGE_STORAGE)
            Files.createDirectories(dir)
            StorageFactory.getLocal(dir, OS.getAppFile(PinkPigMail::class.java, Constants.ACCOUNTS_FILE))
        }

        val uiSettings: UISettings by lazy {
            UISettings.read(Constants.UI_FILE)
        }

        val htmlState = HTMLState()

        init
        {
            //  peer constructors
            Peer.addConstructor(Vocabulary.IMAP_ACCOUNT) { id -> IMAPAccount(id, storage) }
            Peer.addConstructor(Vocabulary.IMAP_FOLDER) { id -> IMAPFolder(id, storage) }
            Peer.addConstructor(Vocabulary.IMAP_MESSAGE) { id -> IMAPMessage(id, storage) }
            Peer.addConstructor(Vocabulary.CALDAV_ACCOUNT) { id -> CalDAVAccount(id, storage) }
            Peer.addConstructor(Vocabulary.CALDAV_CALENDAR) { id -> CalDAVCalendar(id, storage) }
            Peer.addConstructor(Vocabulary.CALDAV_EVENT) { id -> CalDAVEvent(id, storage) }
            //  peer roots
            Peer.addRoot(Vocabulary.IMAP_ACCOUNT) { id -> accounts.add(id as IAccount) }
            Peer.addRoot(Vocabulary.CALDAV_ACCOUNT) { id -> accounts.add(id as IAccount) }
        }

        val service: ExecutorService = Executors.newCachedThreadPool()
    }

    //  all UI model updates go through this code
    override fun delta(added: Model, deleted: Model)
    {
//        Peer.delta(added, deleted)
//    }
        Peer.delta(added, deleted) {
            //   it.subject.toString().contains("orange") &&
            it.predicate.toString().contains("startsAt") || it.predicate.toString().contains("endsAt")
            //  && it.`object`.toString().contains("Event")
        }
    }

    private val appToolBar = HBox()
    private val rooTabPane = TabPane()
    private val root = VBox(appToolBar, rooTabPane)
    private val bootPane = WaitSpinner("Synchronizing Accounts -- Please Wait")
    private val shutdownPane = WaitSpinner("Closing Accounts -- Please Wait")
    private val bootProperty = SimpleIntegerProperty()
    private val mainFlipper = Flipper(bootProperty, 4000.0)

    init
    {
        //booting.setBackground(Background(BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)))
        mainFlipper.addNode(2, shutdownPane)
        mainFlipper.addNode(1, root)
        mainFlipper.addNode(0, bootPane)
        bootProperty.set(0)
    }

    private fun loadAhead(fvm: FolderViewModel)
    {
        val model = fvm.selectionModel ?: return
        assert(model.selectedItems.isNotEmpty())

        val pos = model.selectedIndices[model.selectedIndices.size - 1]
        //  TODO -- should do better than this, expand outwards, especially if we have a multi-selection
        //val messages = LinkedList<IMessage>()
        for (i in pos - 5 until pos + 5)
        {
            try
            {
                model.tableView.items[i].loadAhead()
            } catch (e: IndexOutOfBoundsException)
            {
                //  ignore -- this can happen and it's ok
            }
        }

//        messages[0].folder.loadAhead(messages)
    }

    private fun displayMessage(messages: List<IMessage>, messageView: MessageView)
    {
        if (messages.size == 1)
        {
            val message = messages[messages.size - 1]
            htmlState.message = message
            messageView.setMessage(message)
            //	don't mark junk as read
            if (message.mailAccount.isDisplayMessageMarksAsRead && !message.junkProperty.get())
            {
                message.folder.markMessagesAsRead(listOf(message))
            }
        }
    }

    private fun createPerAccountToolBar(pad: AccountViewModel): HBox
    {
//        val config = ActionHelper.create(Icons.configure(),
//                { Actions.configureAccount(pad.mailAccount) }, Strings.CONFIGURE_ACCOUNT, false)
//      val layout = ActionHelper.create(Icons.switchHorizontal(),
//                {
//                    val p = pad.currentFolderViewModel().visiblePerspective
//                    p.set(if (p.get() == FolderSettings.HORIZONTAL_VIEW) FolderSettings.VERTICAL_VIEW else FolderSettings.HORIZONTAL_VIEW)
//                }, Strings.SWITCH_HORIZONTAL, false)
        val reply = ActionHelper.create(Icons.reply(), { Actions.replyToMessage(pad.currentMessage(), false) }, Strings.REPLY)
        val replyAll = ActionHelper.create(Icons.replyAll(), { Actions.replyToMessage(pad.currentMessage(), true) }, Strings.REPLY_ALL)
        val forward = ActionHelper.create(Icons.forward(), { Actions.forwardMail(pad.currentMessage()) }, Strings.FORWARD)
        val compose = ActionHelper.create(Icons.compose(), { Actions.composeMail(pad.mailAccount) }, Strings.COMPOSE, false)
        val delete = ActionHelper.create(Icons.delete(),
                {
                    //  move to the next message
                    val indices = pad.currentFolderViewModel().selectionModel!!.selectedIndices
                    Actions.deleteMessages(pad.currentMessages())
                    pad.currentFolderViewModel().selectionModel!!.clearAndSelect(if (indices.isEmpty()) 0 else indices[indices.size - 1] + 1)
                }, Strings.DELETE)
        val markJunk = ActionHelper.create(Icons.markJunk(),
                {
                    //  move to the next message
                    val indices = pad.currentFolderViewModel().selectionModel!!.selectedIndices
                    Actions.markMessagesAsJunk(pad.currentMessages())
                    pad.currentFolderViewModel().selectionModel!!.clearAndSelect(if (indices.isEmpty()) 0 else indices[indices.size - 1] + 1)
                }, Strings.MARK_JUNK)
        val markNotJunk = ActionHelper.create(Icons.markNotJunk(),
                { Actions.markMessagesAsNotJunk(pad.currentMessages()) }, Strings.MARK_NOT_JUNK)

        val singleMessageActions = arrayOf(reply, replyAll, forward)
        val multiMessageActions = arrayOf(delete, markJunk, markNotJunk)

        val spacer1 = UIUtils.spacer()
        HBox.setHgrow(spacer1, Priority.ALWAYS)
        val spacer2 = UIUtils.spacer()
        HBox.setHgrow(spacer2, Priority.ALWAYS)

        val replyGroup = HBox(ButtonHelper.regular(reply), ButtonHelper.regular(replyAll), ButtonHelper.regular(forward))
        replyGroup.spacing = 2.0
        val markGroup = HBox(ButtonHelper.regular(delete), ButtonHelper.regular(markJunk), ButtonHelper.regular(markNotJunk))
        markGroup.spacing = 2.0

        val middleButtons = HBox(ButtonHelper.regular(compose), replyGroup, markGroup)
        middleButtons.spacing = 15.0

        //val configButton = ButtonHelper.regular(config)
//        val layoutButton = ButtonHelper.regular(layout)

        val toolBar = HBox()

        toolBar.children.addAll(spacer1, middleButtons, spacer2)
        toolBar.padding = Insets(1.0, 0.0, 1.0, 0.0)
        toolBar.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)

        VBox.setVgrow(toolBar, Priority.NEVER)

        pad.messagesSelected.subscribe { fvm ->
            singleMessageActions.forEach { it.disabledProperty().set(fvm.selectionModel!!.selectedIndices.size != 1) }
            multiMessageActions.forEach { it.disabledProperty().set(fvm.selectionModel!!.selectedIndices.isEmpty()) }
        }

        return toolBar
    }

    private fun createPerAccountHeaderView(fvm: FolderViewModel, folder: IFolder,
                                           messageView: MessageView): TableView<IMessage>
    {
        val sortedList = SortedList(folder.messages)

        val cellValueFactory = { param: CellDataFeatures<IMessage, IMessage> -> ReadOnlyObjectWrapper(param.value) }

        val headersView = TableView(sortedList)
        headersView.columnResizePolicy = SmartResize.POLICY
        UIUtils.resizable(headersView)

        sortedList.comparatorProperty().bind(headersView.comparatorProperty())

        if (fvm.selectionModel == null)
        {
            headersView.selectionModel.selectionMode = SelectionMode.MULTIPLE
            fvm.selectionModel = headersView.selectionModel
        } else
        {
            headersView.selectionModel = fvm.selectionModel
        }

        headersView.selectionModel.selectedItems.addListener { c: Change<out IMessage> ->
            while (c.next())
            {
                if (c.wasAdded() && c.addedSize != 0)
                {
                    displayMessage(c.list, messageView)
                }
            }
        }

        headersView.selectionModel.selectedCells.addListener { c: Change<out TablePosition<*, *>> ->
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
            setCellFactory { AddressCell(folder.mailAccount) { it.from } }
            prefWidth = 300.0
            //  TODO
            comparator = Comparators.cmp<EmailAddress> { it.from[0] }
        }

        val receivedCol = TableColumn<IMessage, IMessage>(Strings.RECEIVED)
        receivedCol.setCellValueFactory(cellValueFactory)
        with(receivedCol) {
            setCellFactory { DateCell { it.receivedOnProperty } }
            prefWidth = 200.0
            comparator = Comparators.cmp<LocalDate> { it.receivedOnProperty.get() }
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

        headersView.columns.addAll(statusColumn, fromColumn, subjectCol, receivedCol)

//        sort on date received --why we need to do this when the thing is a sorted list?
//        possibly due to data streaming in ....
        headersView.sortOrder.add(receivedCol)

        return headersView
    }

    private fun createFolderPerspective(fvm: FolderViewModel, folder: IFolder, @Suppress("SameParameterValue") orientation: Orientation): SplitPane
    {
        val messageView = MessageView(service)
        UIUtils.resizable(messageView)
        val splitPane = SplitPane(createPerAccountHeaderView(fvm, folder, messageView), messageView)
        splitPane.orientation = orientation
        UIUtils.resizable(splitPane)
        return splitPane
    }

    private fun createFolderView(fvm: FolderViewModel, folder: IFolder): Flipper<String>
    {
        val fSettings = uiSettings.getFolderSettings(folder)

        val hPerspective = createFolderPerspective(fvm, folder, Orientation.VERTICAL)
        //  val vPerspective = createFolderPerspective(fvm, folder, Orientation.HORIZONTAL)

        //  this is horrible
        hPerspective.dividers[0].position = fSettings.horizontalPositions[0].position
        // vPerspective.dividers[0].position = fSettings.verticalPositions[0].position

        Bindings.bindContent<SplitPane.Divider>(fSettings.horizontalPositions, hPerspective.dividers)
        //  Bindings.bindContent<SplitPane.Divider>(fSettings.verticalPositions, vPerspective.dividers)

        val flipper = Flipper<String>(fvm.visiblePerspective)
        UIUtils.resizable(flipper)

        flipper.addNode(FolderSettings.HORIZONTAL_VIEW, hPerspective)
        //flipper.addNode(FolderSettings.VERTICAL_VIEW, vPerspective)
        //  set the default view type
        fvm.visiblePerspective.set(fSettings.viewType)

        return flipper
    }

    private fun createAccountView(pad: AccountViewModel, accountsRoot: TreeItem<IFolder>): TreeView<IFolder>
    {
        val accountView = TreeView(accountsRoot)
        accountView.isShowRoot = false
        accountView.setCellFactory { FolderCell() }
        UIUtils.resizable(accountView)

        //  publish events -- new folder selected (single selection model)
        EventStreams.changesOf(accountView.selectionModel.selectedItems)
                .filter { it.list.size == 1 }
                .subscribe { pad.folderSelected.push(it.list[0].value) }

        return accountView
    }

    private fun initTab(box: Node, icon: Glyph, label: StringProperty): Tab
    {
        val tab = Tab()
        with(tab) {
            content = box
            graphic = icon
            textProperty().bind(label)
            closableProperty().set(false)
        }

        return tab
    }

    private fun addCalendarView(@Suppress("UNUSED_PARAMETER") primaryStage: Stage, account: ICalendarAccount)
    {
        val calendarView = CalendarView()
        calendarView.calendarSources.add(account.source)
        calendarView.requestedTime = LocalTime.now()
        rooTabPane.tabs.add(initTab(calendarView, Icons.calendar(), account.nickNameProperty))
    }

    private fun addMailView(primaryStage: Stage, account: IEmailAccount)
    {
        val pad = AccountViewModel(account)

        val accountsRoot = TreeItem<IFolder>()
        val accountView = createAccountView(pad, accountsRoot)
        val toolBar = createPerAccountToolBar(pad)

        val folderViews = Flipper<IFolder>(pad.visibleView)
        UIUtils.resizable(folderViews)

        val folderArea = SplitPane(accountView, folderViews)
        UIUtils.resizable(folderArea)

        //  when a folder is added to the accounts folder list add an item to the account view
        //  and add a folder view for the folder

        account.folders.addListener { c: Change<out IFolder> ->
            //            println("Adding folders" + mailAccount)
            while (c.next())
            {
                c.addedSubList.forEach {
                    //                    println("Actually Adding folder" + it)
                    val fvm = FolderViewModel()
                    pad.addFolderViewModel(it, fvm)
                    val treeItem = TreeItem<IFolder>(it)
                    accountsRoot.children.add(treeItem)
                    val flipper = createFolderView(fvm, it)
                    //  publish events -- new message selected
                    EventStreams.changesOf((fvm.selectionModel
                            ?: return@forEach).selectedIndices).subscribe { pad.messagesSelected.push(fvm) }

                    folderViews.addNode(it, flipper)
                    if (it.isInbox)
                    {
                        Platform.runLater { accountView.selectionModel.select(treeItem) }
                    }
                }
            }
        }

        val box = VBox(toolBar, folderArea)
        UIUtils.resizable(box)
        VBox.setVgrow(toolBar, Priority.NEVER)
        VBox.setVgrow(folderArea, Priority.ALWAYS)

        rooTabPane.tabs.add(initTab(box, Icons.mail(), account.nickNameProperty))

        folderArea.setDividerPositions(uiSettings.verticalPosition[0].position)

        //  TODO is this is necessary to try to work around a JavaFX bug with setting split pane positions?
        primaryStage.setOnShown {
            Platform.runLater {
                folderArea.setDividerPositions(uiSettings.verticalPosition[0].position)
                Bindings.bindContent<SplitPane.Divider>(uiSettings.verticalPosition, folderArea.dividers)
            }
        }

        //  message load-ahead
        pad.messagesSelected.filter { it.selectionModel!!.selectedIndices.isNotEmpty() }.subscribe { loadAhead(it) }
    }

    private val viewCreator = mapOf(
            IMAPAccount::class to { stage: Stage, account: IAccount -> addMailView(stage, account as IEmailAccount) },
            CalDAVAccount::class to { stage, account -> addCalendarView(stage, account as ICalendarAccount) })

    private fun saveUISettings()
    {
        val names = NameSource(Vocabulary.NBASE)
        val uiModel = ModelFactory.createDefaultModel()
        uiModel.setNsPrefix("n", Vocabulary.NBASE)
        uiModel.setNsPrefix("o", Vocabulary.TBASE)
        accounts.forEach { uiSettings.save(uiModel, names, it) }
        RDFDataMgr.write(Files.newOutputStream(OS.getAppFile(PinkPigMail::class.java, Constants.UI_FILE)), uiModel, Lang.TURTLE)
    }

    //  shutdown sequence
    private fun shutdown(@Suppress("UNUSED_PARAMETER") event: WindowEvent)
    {
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> ; }
        bootProperty.set(2)
        val t = Thread {
            try
            {
                saveUISettings()
            } catch (ex: Exception)
            {
                ex.printStackTrace()
                //  ignore
            }

            //  shutdown the storage layer
            try
            {
                storage.close()
            } catch (ex: Exception)
            {
                ex.printStackTrace()
                //  ignore
            }
            //                RDFDataMgr.write(Files.newOutputStream(OS.getAppFile(PinkPigMail::class.java, Constants.ACCOUNTS_FILE)), accountsModel, Lang.TURTLE)
        }

        t.start()
        t.join()
        exitProcess(1)
    }

    //  boot sequence
    override fun start(primaryStage: Stage)
    {
        Thread.setDefaultUncaughtExceptionHandler(ErrorHandler())
        //  TODO -- have to make this work
        URL.setURLStreamHandlerFactory(CustomURLStreamHandlerFactory(htmlState))

        UIUtils.resizable(rooTabPane)
        UIUtils.resizable(root)
        UIUtils.resizable(bootPane)
        UIUtils.resizable(shutdownPane)
        UIUtils.resizable(mainFlipper)

        VBox.setVgrow(rooTabPane, Priority.ALWAYS)
        VBox.setVgrow(appToolBar, Priority.NEVER)

        with(primaryStage) {
            scene = Scene(mainFlipper)
            scene.stylesheets.add(PinkPigMail::class.java.getResource(STYLE_SHEET).toExternalForm())
            title = "Pink Pig Mail"
            icons.add(Image(Icons.thePig128()))
            width = uiSettings.widthProperty.get()
            height = uiSettings.heightProperty.get()
            setOnCloseRequest(::shutdown)
        }

        uiSettings.widthProperty.bind(primaryStage.widthProperty())
        uiSettings.heightProperty.bind(primaryStage.heightProperty())

        //  on adding of a new account, add an account view for it
        accounts.addListener { c: Change<out IAccount> ->
            while (c.next())
            {
                c.addedSubList.forEach {
                    Platform.runLater { viewCreator[it::class]?.let { it1 -> it1(primaryStage, it) } }
                }
            }
        }

        //  sync and switch to main pane when done
        Thread {
            storage.addListener(this).forEach { it.value.get() }
            Platform.runLater { bootProperty.set(1) }
        }.start()

        primaryStage.show()
    }
}