package org.knowtiphy.pinkpigmail

import com.calendarfx.view.CalendarView
import javafx.application.Application
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.image.Image
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.stage.WindowEvent
import org.apache.jena.arq.querybuilder.SelectBuilder
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.controlsfx.glyphfont.Glyph
import org.knowtiphy.babbage.storage.*
import org.knowtiphy.owlorm.javafx.PeerState
import org.knowtiphy.pinkpigmail.mailaccountview.MailAccountView
import org.knowtiphy.pinkpigmail.mailview.CustomURLStreamHandlerFactory
import org.knowtiphy.pinkpigmail.mailview.HTMLState
import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.ICalendarAccount
import org.knowtiphy.pinkpigmail.model.IContactAccount
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.caldav.*
import org.knowtiphy.pinkpigmail.model.imap.IMAPAccount
import org.knowtiphy.pinkpigmail.model.storage.DavStorage
import org.knowtiphy.pinkpigmail.model.storage.MailStorage
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.ErrorHandler
import org.knowtiphy.pinkpigmail.util.ui.StorageEvent
import org.knowtiphy.pinkpigmail.util.ui.UIUtils
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.later
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.resizeable
import org.knowtiphy.utils.IProcedure.doAndIgnore
import org.knowtiphy.utils.NameSource
import org.knowtiphy.utils.OS
import org.reactfx.EventSource
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.logging.LogManager
import kotlin.system.exitProcess

/**
 * @author graham
 */

class PinkPigMail : Application()
{
	companion object
	{
		private const val UI_FILE = "ui.ttl"
		const val STYLE_SHEET = "/styles.css"

		private val GET_ACCOUNTS: String = SelectBuilder()
			.addVar("*")
			.addWhere("?aid", "<${RDF.type}>", "?type")
			.addWhere("?type", "<${RDFS.subClassOf}>", "<${Vocabulary.ACCOUNT}>")
			.addFilter("?type != <${Vocabulary.ACCOUNT}>")
			.buildString()

		private val EVENT_IDS: String = SelectBuilder()
			.addVar("*")
			.addWhere("?eid", "<${RDF.type}>", "?type")
			.addWhere("?type", "<${RDFS.subClassOf}>", "<${Vocabulary.EVENT}>")
			.addWhere("?eid", "<${Vocabulary.HAS_ACCOUNT}>", "?aid")
			.addFilter("?type != <${Vocabulary.EVENT}>")
			.buildString()

		private val accounts: ObservableMap<String, IAccount> = FXCollections.observableHashMap()

		private val storage: IStorage by lazy { StorageFactory.getLocal() }
		private val service: ExecutorService = Executors.newCachedThreadPool()

		//	shared globals

		val uiSettings: UISettings by lazy { UISettings.read(UI_FILE) }

		val htmlState = HTMLState()

		//	executor pool for doing periodic tasks like updating the current time in calendar views
		val timerService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

		val nameSource = NameSource(Vocabulary.NBASE)

		//	all events are posted here
		val events = EventSource<StorageEvent>()
	}

	private val appToolBar = HBox()
	private val rootTabPane = resizeable(TabPane())
	private val root = resizeable(VBox(appToolBar, rootTabPane))

	init
	{
		val stream = PinkPigMail::class.java.getResource("/logging.properties")!!.openStream()
		try
		{
			LogManager.getLogManager().readConfiguration(stream)
		} catch (ex: IOException)
		{
			ex.printStackTrace()
		}

		//  peer constructors for roots -- this goes away with the event handling framework
		//	only need roots
		PeerState.addConstructor(Vocabulary.IMAP_ACCOUNT) { IMAPAccount(it, MailStorage(storage)) }
		PeerState.addConstructor(Vocabulary.CALDAV_ACCOUNT) { CalDAVAccount(it, DavStorage(storage)) }
		PeerState.addConstructor(Vocabulary.CARDDAV_ACCOUNT) { CardDAVAccount(it, DavStorage(storage)) }

		PeerState.addConstructor(Vocabulary.CALDAV_CALENDAR) { CalDAVCalendar(it, DavStorage(storage)) }
		PeerState.addConstructor(Vocabulary.CALDAV_EVENT) { CalDAVEvent(it, DavStorage(storage)) }
		PeerState.addConstructor(Vocabulary.CARDDAV_ADDRESSBOOK) { CardDAVAddressBook(it, DavStorage(storage)) }
		PeerState.addConstructor(Vocabulary.CARDDAV_GROUP) { CardDAVGroup(it, DavStorage(storage)) }
		PeerState.addConstructor(Vocabulary.CARDDAV_CARD) { CardDAVCard(it, DavStorage(storage)) }

		VBox.setVgrow(rootTabPane, Priority.ALWAYS)
		VBox.setVgrow(appToolBar, Priority.NEVER)
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

	private fun addCalendarView(account: ICalendarAccount)
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

	private fun addCardView(account: IContactAccount)
	{
		rootTabPane.tabs.add(createAccountTab(ContactView(account), Icons.book(), account.nickNameProperty))
	}

	private fun addMailView(account: IEmailAccount)
	{
		rootTabPane.tabs.add(
			createAccountTab(MailAccountView(service, account), Icons.mail(), account.nickNameProperty)
		)
	}

	private val createAccountView = mapOf(
		IMAPAccount::class to { account: IAccount -> addMailView(account as IEmailAccount) },
		CalDAVAccount::class to { account -> addCalendarView(account as ICalendarAccount) },
		CardDAVAccount::class to { account -> addCardView(account as IContactAccount) })

	private fun saveUISettings()
	{
		val model = uiSettings.save(accounts.values)
		RDFDataMgr.write(
			Files.newOutputStream(
				Paths.get(OS.getSettingsDir(PinkPigMail::class.java).toString(), UI_FILE)
			),
			model, Lang.TURTLE
		)
	}

	//  shutdown sequence
	//	done on a thread, so the window closes immediately while shutdown goes ahead in the background
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

	//	build the known accounts
	private fun buildAccounts()
	{
		QueryExecutionFactory.create(GET_ACCOUNTS, ModelFactory.createRDFSModel(storage.accounts)).execSelect()
			.forEach {
				val aid = it.getResource("aid").toString()
				val account = PeerState.construct(aid, it.getResource("type").toString()) as IAccount
				account.initialize()
				accounts[aid] = account
			}
	}

	//	channel listener events to the per account and global event streams
	private fun eventHandler(eventModel: Model)
	{
		val infModel = ModelFactory.createRDFSModel(eventModel)
		QueryExecutionFactory.create(EVENT_IDS, infModel).execSelect().forEach {
			val event = StorageEvent(it.get("eid"), it.get("type"), it.get("aid"), infModel)
			//	by doing the later it puts the whole event stream on the FX UI thread
			later {
				//	push the event to the account's event stream
				if (event.aid != null)
					accounts[event.aid]?.events!!.push(event)
				//	push the event to the all events stream
				events.push(event)
			}
		}
	}

	//  boot sequence
	override fun start(stage: Stage)
	{
		//	set up an error handler for uncaught exceptions
		Thread.setDefaultUncaughtExceptionHandler(ErrorHandler())

		//	set up a stream handler to intercept URL loading -- needed to stop loading of images
		//	in mail messages for example
		//	TODO -- this should be done via some Service interface nowdays?
		//	and it should only be used in the mail html viewer (do they expose those features yet?)
		URL.setURLStreamHandlerFactory(CustomURLStreamHandlerFactory(htmlState))

		//	get the known accounts
		buildAccounts()

		//	initialize the stage
		with(stage) {
			scene = UIUtils.getScene(root)
			title = Strings.APP_NAME
			icons.add(Image(Icons.thePig128()))
			width = uiSettings.widthProperty.get()
			height = uiSettings.heightProperty.get()
			setOnCloseRequest(::shutdown)
		}

		uiSettings.widthProperty.bind(stage.widthProperty())
		uiSettings.heightProperty.bind(stage.heightProperty())

		//	create views for each account
		accounts.values.forEach { account -> createAccountView[account::class]?.let { it(account) } }

		//	add an event listener for storage events
		storage.addListener(::eventHandler)

		//	TODO -- need to have some global events -- e.g. lost store connection
		events.subscribe { }

		//	synch each account
		accounts.values.forEach { it.sync() }

		//	show the UI
		stage.show()
	}
}