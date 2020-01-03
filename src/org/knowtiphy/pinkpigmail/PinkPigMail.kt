package org.knowtiphy.pinkpigmail

import com.calendarfx.view.CalendarView
import javafx.application.Application
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.image.Image
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
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
import org.knowtiphy.pinkpigmail.Globals.htmlState
import org.knowtiphy.pinkpigmail.Globals.timerService
import org.knowtiphy.pinkpigmail.mailaccountview.MailAccountView
import org.knowtiphy.pinkpigmail.mailview.CustomURLStreamHandlerFactory
import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.ICalendarAccount
import org.knowtiphy.pinkpigmail.model.IContactAccount
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
import org.knowtiphy.utils.IProcedure.doAndIgnore
import org.knowtiphy.utils.NameSource
import org.knowtiphy.utils.OS
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
		}
		catch (ex: IOException)
		{
			ex.printStackTrace()
		}
	}

	companion object
	{
		//		private const val MESSAGE_STORAGE = "messages"
//		private const val ACCOUNTS_FILE = "accounts.ttl"
		private const val UI_FILE = "ui.ttl"

		const val STYLE_SHEET = "styles.css"

		private val accounts: ObservableList<IAccount> = FXCollections.observableArrayList()

		val storage: IStorage by lazy { StorageFactory.getLocal() }

		val uiSettings: UISettings by lazy { UISettings.read(UI_FILE) }

		private val service: ExecutorService = Executors.newCachedThreadPool()

		private val mainFlipper = resizeable(Replacer())
		private val appToolBar = HBox()
		private val rootTabPane = resizeable(TabPane())
		private val root = resizeable(VBox(appToolBar, rootTabPane))
		private val bootPaneSpinner = WaitSpinner(Strings.SYNCHRONIZING_ACCOUNTS).resume()
		private val bootPane = resizeable(UIUtils.boxIt(bootPaneSpinner))
	}

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

		mainFlipper.children.addAll(root, bootPane)
	}

	//  all UI model updates go through this code
	override fun delta(added: Model, deleted: Model)
	{
		//  PeerState.delta(added, deleted)
		PeerState.delta(added, deleted) {
			it.predicate.toString().contains(Vocabulary.HAS_GROUP)
			//   it.subject.toString().contains("orange") &&
			// it.`object`.toString().contains("2c809517-316a-4aa7-968f-e29178f7c244")//|| it.`object`.toString().contains("CARD")
			//  && it.`object`.toString().contains("Event")
		}
	}

	private fun createAccountTab(tabContent: Region, icon: Glyph, label: StringProperty): Tab
	{
		val tab = Tab()
		with(tab) {
			content = resizeable(tabContent)
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
		val now = LocalTime.now()
		val initialDelay = 60L - now.second
		calendarView.requestedTime = now
		timerService.scheduleAtFixedRate({
			later {
				val dateTime = LocalDateTime.now()
				calendarView.today = dateTime.toLocalDate()
				calendarView.time = dateTime.toLocalTime()
			}
		}, initialDelay, 60L, TimeUnit.SECONDS)
		rootTabPane.tabs.add(createAccountTab(calendarView, Icons.calendar(), account.nickNameProperty))
	}

	private fun addCardView(@Suppress("UNUSED_PARAMETER") primaryStage: Stage, account: IContactAccount)
	{
		rootTabPane.tabs.add(createAccountTab(ContactView(account), Icons.book(), account.nickNameProperty))
	}

	private fun addMailView(primaryStage: Stage, account: IEmailAccount)
	{
		rootTabPane.tabs.add(createAccountTab(MailAccountView(primaryStage, service, account), Icons.mail(), account.nickNameProperty))
	}

	private val viewCreator = mapOf(
			IMAPAccount::class to { stage: Stage, account: IAccount -> addMailView(stage, account as IEmailAccount) },
			CalDAVAccount::class to { stage, account -> addCalendarView(stage, account as ICalendarAccount) },
			CardDAVAccount::class to { stage, account -> addCardView(stage, account as IContactAccount) })

	private fun saveUISettings()
	{
		val names = NameSource(Vocabulary.NBASE)
		val uiModel = ModelFactory.createDefaultModel()
		uiModel.setNsPrefix("n", Vocabulary.NBASE)
		uiModel.setNsPrefix("o", Vocabulary.TBASE)
		accounts.forEach { uiSettings.save(uiModel, names, it) }
		RDFDataMgr.write(Files.newOutputStream(Paths.get(OS.getSettingsDir(PinkPigMail::class.java).toString(), UI_FILE)), uiModel, Lang.TURTLE)
	}

	//  shutdown sequence
	//	- save UI settings
	//  - shutdown the storage layer
	//                RDFDataMgr.write(Files.newOutputStream(OS.getAppFile(PinkPigMail::class.java, Constants.ACCOUNTS_FILE)), accountsModel, Lang.TURTLE)
	private fun shutdown(@Suppress("UNUSED_PARAMETER") event: WindowEvent)
	{
		Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
		Thread {
			doAndIgnore { timerService.shutdown() }
			doAndIgnore { timerService.awaitTermination(10, TimeUnit.SECONDS) }
			doAndIgnore { service.shutdown() }
			doAndIgnore { service.awaitTermination(10, TimeUnit.SECONDS) }
			doAndIgnore(::saveUISettings)
			doAndIgnore(storage::close)
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
			scene = UIUtils.getScene(mainFlipper)
			title = Strings.APP_NAME
			icons.add(Image(Icons.thePig128()))
			width = uiSettings.widthProperty.get()
			height = uiSettings.heightProperty.get()
			setOnCloseRequest(::shutdown)
		}

		uiSettings.widthProperty.bind(primaryStage.widthProperty())
		uiSettings.heightProperty.bind(primaryStage.heightProperty())

		//  sync and switch to main pane when done
		Thread {
			storage.addListener(this).forEach { it.value.get() }
			//	TODO -- these two lines merge when the new synch setup is done
			accounts.forEach { later { viewCreator[it::class]?.let { it1 -> it1(primaryStage, it) } } }
			//  synch has finished -- publish an event for it
			accounts.forEach { later { Globals.synched.push(it) } }
			later {
				bootPaneSpinner.finish()
				mainFlipper.flip(root)
			}
			//  on adding of a new account, add an account view for it
			accounts.addListener { c: ListChangeListener.Change<out IAccount> ->
				while (c.next())
				{
					if (c.wasAdded())
						c.addedSubList.forEach { later { viewCreator[it::class]?.let { it1 -> it1(primaryStage, it) } } }
				}
			}
		}.start()

		primaryStage.show()
	}
}



//class Foo(private val xxx: String, private val n : Int) : Callable<Void?>, PriorityExecutor.Important
//{
//	override fun call(): Void?
//	{
//		println("FOOO " + xxx)
//		return null
//	}
//
//	override fun getPriority(): Int
//	{
//		return n
//	}
//}
//		workQ = new LinkedBlockingDeque<>();
//		val s = PriorityExecutor(CustomThreadFactory("WorkQ"))
//		s.submit {
//			println("X1")
//			Thread.sleep(5000)
//			println("X1 Done")
//		}
//		s.submit {
//			println("X2")
//		}
//		s.submit {
//			println("X3")
//		}
//
//		s.submit(Foo("AAA",10))
//		s.submit(Foo("BBB", 10))
//		s.submit(Foo("CCC", 5))
//		s.submit(Foo("DDD", 20))
//
//		Thread.sleep(20000)
//		exitProcess(1)