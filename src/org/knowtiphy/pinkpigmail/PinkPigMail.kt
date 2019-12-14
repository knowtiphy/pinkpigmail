package org.knowtiphy.pinkpigmail

import com.calendarfx.view.CalendarView
import javafx.application.Application
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
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.ErrorHandler
import org.knowtiphy.pinkpigmail.util.ui.Replacer
import org.knowtiphy.pinkpigmail.util.ui.UIUtils
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.later
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.resizeable
import org.knowtiphy.pinkpigmail.util.ui.WaitSpinner
import org.knowtiphy.utils.NameSource
import org.knowtiphy.utils.OS
import org.reactfx.EventSource
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
        private const val ACCOUNTS_FILE = "accounts.ttl"
        private const val UI_FILE = "ui.ttl"
        private const val STYLE_SHEET = "styles.css"

        val synched = EventSource<IAccount>()
        val accounts: ObservableList<IAccount> = FXCollections.observableArrayList()

        val storage: IStorage by lazy {
            val dir = Paths.get(OS.getAppDir(PinkPigMail::class.java).toString(), MESSAGE_STORAGE)
            Files.createDirectories(dir)
            StorageFactory.getLocal(dir, OS.getAppFile(PinkPigMail::class.java, ACCOUNTS_FILE))
        }

        val uiSettings: UISettings by lazy {
            UISettings.read(UI_FILE)
        }

        val htmlState = HTMLState()

        private val appToolBar = HBox()
        private val rootTabPane = resizeable(TabPane())
        private val root = resizeable(VBox(appToolBar, rootTabPane))
        private val bootPane = resizeable(UIUtils.boxIt(WaitSpinner(Strings.SYNCHRONIZING_ACCOUNTS)))
        private val shutdownPane = resizeable(UIUtils.boxIt(WaitSpinner(Strings.CLOSING_ACCOUNTS)))
        private val mainFlipper = resizeable(Replacer())

        init
        {
            //  peer constructors
            PeerState.addConstructor(Vocabulary.IMAP_ACCOUNT) { IMAPAccount(it, storage) }
            PeerState.addConstructor(Vocabulary.IMAP_FOLDER) { IMAPFolder(it, storage) }
            PeerState.addConstructor(Vocabulary.IMAP_MESSAGE) { IMAPMessage(it, storage) }
            PeerState.addConstructor(Vocabulary.CALDAV_ACCOUNT) { CalDAVAccount(it, storage) }
            PeerState.addConstructor(Vocabulary.CALDAV_CALENDAR) { CalDAVCalendar(it, storage) }
            PeerState.addConstructor(Vocabulary.CALDAV_EVENT) { CalDAVEvent(it, storage) }
            PeerState.addConstructor(Vocabulary.CARDDAV_ACCOUNT) { CardDAVAccount(it, storage) }
            PeerState.addConstructor(Vocabulary.CARDDAV_ADDRESSBOOK) { CardDAVAddressBook(it, storage) }
            PeerState.addConstructor(Vocabulary.CARDDAV_GROUP) { CardDAVGroup(it, storage) }
            PeerState.addConstructor(Vocabulary.CARDDAV_CARD) { CardDAVCard(it, storage) }
            //  peer roots
            PeerState.addRoot(Vocabulary.IMAP_ACCOUNT) { accounts.add(it as IAccount) }
            PeerState.addRoot(Vocabulary.CALDAV_ACCOUNT) { accounts.add(it as IAccount) }
            PeerState.addRoot(Vocabulary.CARDDAV_ACCOUNT) { accounts.add(it as IAccount) }

            mainFlipper.children.addAll(shutdownPane, root, bootPane)
        }

        val service: ExecutorService = Executors.newCachedThreadPool()
    }

    //  all UI model updates go through this code
    override fun delta(added: Model, deleted: Model)
    {
        //  PeerState.delta(added, deleted)
        PeerState.delta(added, deleted) {
            //   it.subject.toString().contains("orange") &&
            it.`object`.toString().contains("2c809517-316a-4aa7-968f-e29178f7c244")//|| it.`object`.toString().contains("CARD")
            //  && it.`object`.toString().contains("Event")
        }
    }

    private fun tabIt(box: Node, icon: Glyph, label: StringProperty): Tab
    {
        val tab = Tab()
        with(tab) {
            content = box
            graphic = icon
            textProperty().bind(label)
            textProperty().bind(label)
            closableProperty().set(false)
        }

        return tab
    }

    private fun addCalendarView(@Suppress("UNUSED_PARAMETER") primaryStage: Stage, account: ICalendarAccount)
    {
        val calendarView = resizeable(CalendarView())
        calendarView.calendarSources.add(account.source)
        calendarView.requestedTime = LocalTime.now()
        //  TODO -- the thread to update the requested time
        rootTabPane.tabs.add(tabIt(calendarView, Icons.calendar(Icons.DEFAULT_SIZE), account.nickNameProperty))
    }

    private fun addCardView(@Suppress("UNUSED_PARAMETER") primaryStage: Stage, account: ICardAccount)
    {
        rootTabPane.tabs.add(tabIt(resizeable(ContactView(account)),
                Icons.book(Icons.DEFAULT_SIZE), account.nickNameProperty))
    }

    private fun addMailView(primaryStage: Stage, account: IEmailAccount)
    {
        rootTabPane.tabs.add(tabIt(resizeable(MailAccountView(primaryStage, account)),
                Icons.mail(Icons.DEFAULT_SIZE), account.nickNameProperty))
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
        RDFDataMgr.write(Files.newOutputStream(OS.getAppFile(PinkPigMail::class.java, UI_FILE)), uiModel, Lang.TURTLE)
    }

    //  shutdown sequence
    private fun shutdown(@Suppress("UNUSED_PARAMETER") event: WindowEvent)
    {
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
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

    //  boot sequence
    override fun start(primaryStage: Stage)
    {
        Thread.setDefaultUncaughtExceptionHandler(ErrorHandler())
        URL.setURLStreamHandlerFactory(CustomURLStreamHandlerFactory(htmlState))

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
                if (c.wasAdded())
                    c.addedSubList.forEach { later { viewCreator[it::class]?.let { it1 -> it1(primaryStage, it) } } }
            }
        }

        //  sync and switch to main pane when done
        Thread {
            storage.addListener(this).forEach { it.value.get() }
            //  synch has finished -- publish an event for it
            accounts.forEach { later { synched.push(it) } }
            later {
                //((bootPane.children[0] as BorderPane).center as WaitSpinner).progressIndicator.progress = 1.0;
                mainFlipper.flip(root)
            }
        }.start()

        primaryStage.show()
    }
}