package org.knowtiphy.pinkpigmail

import com.calendarfx.view.CalendarView
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener.Change
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.image.Image
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.media.AudioClip
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
import org.knowtiphy.owlorm.javafx.PeerState
import org.knowtiphy.pinkpigmail.mailview.CustomURLStreamHandlerFactory
import org.knowtiphy.pinkpigmail.mailview.HTMLState
import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.ICalendarAccount
import org.knowtiphy.pinkpigmail.model.ICardAccount
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.caldav.*
import org.knowtiphy.pinkpigmail.model.imap.IMAPAccount
import org.knowtiphy.pinkpigmail.model.imap.IMAPFolder
import org.knowtiphy.pinkpigmail.model.imap.IMAPMessage
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Resources
import org.knowtiphy.pinkpigmail.util.Flipper
import org.knowtiphy.pinkpigmail.util.UIUtils
import org.knowtiphy.utils.NameSource
import org.knowtiphy.utils.OS
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
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

        private val appToolBar = HBox()
        private val rootTabPane = TabPane()
        private val root = VBox(appToolBar, rootTabPane)
        private val bootPane = UIUtils.boxIt(WaitSpinner("Synchronizing Accounts"))
        private val shutdownPane = UIUtils.boxIt(WaitSpinner("Closing Accounts"))
        private val mainFlipper = Flipper()

        init
        {
            //  peer constructors
            PeerState.addConstructor(Vocabulary.IMAP_ACCOUNT) { id -> IMAPAccount(id, storage) }
            PeerState.addConstructor(Vocabulary.IMAP_FOLDER) { id -> IMAPFolder(id, storage) }
            PeerState.addConstructor(Vocabulary.IMAP_MESSAGE) { id -> IMAPMessage(id, storage) }
            PeerState.addConstructor(Vocabulary.CALDAV_ACCOUNT) { id -> CalDAVAccount(id, storage) }
            PeerState.addConstructor(Vocabulary.CALDAV_CALENDAR) { id -> CalDAVCalendar(id, storage) }
            PeerState.addConstructor(Vocabulary.CALDAV_EVENT) { id -> CalDAVEvent(id, storage) }
            PeerState.addConstructor(Vocabulary.CARDDAV_ACCOUNT) { id -> CardDAVAccount(id, storage) }
            PeerState.addConstructor(Vocabulary.CARDDAV_ADDRESSBOOK) { id -> CardDAVAddressBook(id, storage) }
            PeerState.addConstructor(Vocabulary.CARDDAV_GROUP) { id -> CardDAVGroup(id, storage) }
            PeerState.addConstructor(Vocabulary.CARDDAV_CARD) { id -> CardDAVCard(id, storage) }
            //  peer roots
            PeerState.addRoot(Vocabulary.IMAP_ACCOUNT) { id -> accounts.add(id as IAccount) }
            PeerState.addRoot(Vocabulary.CALDAV_ACCOUNT) { id -> accounts.add(id as IAccount) }
            PeerState.addRoot(Vocabulary.CARDDAV_ACCOUNT) { id -> accounts.add(id as IAccount) }

            mainFlipper.children.addAll(shutdownPane, root, bootPane)
        }

        val service: ExecutorService = Executors.newCachedThreadPool()

        private val BEEP: AudioClip? = AudioClip(Resources::class.java.getResource("beep-29.wav").toString())

        fun beep()
        {
            try
            {
                BEEP?.play()
            } catch (ex: Exception)
            {
                Fail.failNoMessage(ex)
            }
        }
    }

    //  all UI model updates go through this code
    override fun delta(added: Model, deleted: Model)
    {
//        Peer.delta(added, deleted)
//    }
        PeerState.delta(added, deleted) {
            //   it.subject.toString().contains("orange") &&
            it.predicate.toString().contains("type" )//|| it.`object`.toString().contains("CARD")
            //  && it.`object`.toString().contains("Event")
        }
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
        //  TODO -- the thread to update the requested time
        rootTabPane.tabs.add(initTab(calendarView, Icons.calendar(Icons.DEFAULT_SIZE), account.nickNameProperty))
    }

    private fun addCardView(@Suppress("UNUSED_PARAMETER") primaryStage: Stage, account: ICardAccount)
    {
        val cardView = ContactView(account)
        UIUtils.resizable(cardView)
        rootTabPane.tabs.add(initTab(cardView, Icons.book(Icons.DEFAULT_SIZE), account.nickNameProperty))
    }

    private fun addMailView(primaryStage: Stage, account: IEmailAccount)
    {
        val mailView = MailAccountView(primaryStage, account)
        UIUtils.resizable(mailView)
        rootTabPane.tabs.add(initTab(mailView, Icons.mail(Icons.DEFAULT_SIZE), account.nickNameProperty))
    }

    private val viewCreator = mapOf(
            IMAPAccount::class to { stage: Stage, account: IAccount -> addMailView(stage, account as IEmailAccount) },
            CalDAVAccount::class to { stage, account -> addCalendarView(stage, account as ICalendarAccount) },
            CardDAVAccount::class to { stage, account -> addCardView(stage, account as ICardAccount) })

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
        mainFlipper.flip(bootPane)
        Thread {
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
            exitProcess(1)
        }.start()
    }

    private fun initPostSynch()
    {
//        var newMessages = false
//        for (account in accounts)
//        {
//            if (account is IEmailAccount)
//                for (folder in account.folders)
//                {
//                    for (message in folder.messages)
//                    {
//                        if (!message.readProperty.get())
//                        {
//                            newMessages = true
//                            break
//                        }
//                    }
//                }
//        }
//
//        println(newMessages)
//        if (newMessages)
//        {
////            readProperty.addListener { _, _, new ->
//            //            if (Patterns.INBOX_PATTERN.matcher(folder.nameProperty.get()).matches() && !new)
////            {
//            try
//            {
//                _beep?.play()
//            } catch (ex: Exception)
//            {
//                Fail.failNoMessage(ex)
//            }
////            }
////        }
//        }
        println("POST SYNCH XXXXXXXXXXXXXXXXXXXXXXX")
    }

    //  boot sequence
    override fun start(primaryStage: Stage)
    {
        Thread.setDefaultUncaughtExceptionHandler(ErrorHandler())
        //  TODO -- have to make this work
        URL.setURLStreamHandlerFactory(CustomURLStreamHandlerFactory(htmlState))

        UIUtils.resizable(rootTabPane)
        UIUtils.resizable(root)
        UIUtils.resizable(bootPane)
        UIUtils.resizable(shutdownPane)
        UIUtils.resizable(mainFlipper)

        VBox.setVgrow(rootTabPane, Priority.ALWAYS)
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
            initPostSynch()
            Platform.runLater { mainFlipper.flip(root) }
        }.start()

        primaryStage.show()
    }
}