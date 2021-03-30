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
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.controlsfx.glyphfont.Glyph
import org.knowtiphy.babbage.storage.*
import org.knowtiphy.pinkpigmail.mailaccountview.MailAccountView
import org.knowtiphy.pinkpigmail.mailview.CustomURLStreamHandlerFactory
import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.ICalendarAccount
import org.knowtiphy.pinkpigmail.model.IContactAccount
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.caldav.*
import org.knowtiphy.pinkpigmail.model.events.StageShowEvent
import org.knowtiphy.pinkpigmail.model.imap.IMAPAccount
import org.knowtiphy.pinkpigmail.model.storage.StorageEvent
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.ErrorHandler
import org.knowtiphy.pinkpigmail.util.ui.UIUtils
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.later
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.resizeable
import org.knowtiphy.utils.IProcedure.doAndIgnore
import org.knowtiphy.utils.JenaUtils
import org.knowtiphy.utils.OS
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.LocalTime
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
		private val GET_ACCOUNTS : String = SelectBuilder().addVar("*").addWhere("?aid", "<${RDF.type}>", "?type")
			.addWhere("?type", "<${RDFS.subClassOf}>", "<${Vocabulary.ACCOUNT}>")
			.addFilter("?type != <${Vocabulary.ACCOUNT}>").buildString()

		private val EVENT_IDS : String = SelectBuilder().addVar("*").addWhere("?eid", "<${RDF.type}>", "?type")
			.addWhere("?type", "<${RDFS.subClassOf}>", "<${Vocabulary.EVENT}>")
			.addWhere("?eid", "<${Vocabulary.HAS_ACCOUNT}>", "?aid").addFilter("?type != <${Vocabulary.EVENT}>")
			.buildString()

		private val storage : IStorage by lazy { StorageFactory.getLocal() }

		//  account constructors
		private val ACCOUNT_CONSTRUCTORS =
			mapOf<String, (String) -> IAccount>(Vocabulary.IMAP_ACCOUNT to { id : String -> IMAPAccount(id, storage) },
				Vocabulary.CALDAV_ACCOUNT to { id -> CalDAVAccount(id, storage) },
				Vocabulary.CARDDAV_ACCOUNT to { id -> CardDAVAccount(id, storage) })
	}

	private val accounts : ObservableMap<String, IAccount> = FXCollections.observableHashMap()

	//  TODO -- why do we have this -- we have no global tool bar buttons
	private val appToolBar = HBox()
	private val rootTabPane = resizeable(TabPane())
	private val root = resizeable(VBox(appToolBar, rootTabPane))

	init
	{
		LogManager.getLogManager().readConfiguration(
			PinkPigMail::class.java.getResource("/logging.properties")!!.openStream()
		)

		VBox.setVgrow(rootTabPane, Priority.ALWAYS)
		VBox.setVgrow(appToolBar, Priority.NEVER)
	}

	//  push storage events to both the global from storage stream and the per account from storage streams
	private fun pushEvent(event : StorageEvent)
	{
		//	by doing the later it puts the whole event stream on the FX UI thread
		later {
			//	push the event to the account's event stream
			if (event.aid != null) accounts[event.aid]?.fromStorage?.push(event)
			//	push the event to the all events stream
			Globals.fromStorage.push(event)
		}
	}

	private fun createAccountTab(tabContent : Region, icon : Glyph, label : StringProperty) : Tab
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

	private fun addCalendarView(account : ICalendarAccount)
	{
		val calendarView = CalendarView()
		calendarView.calendarSources.add(account.source)
		val now = LocalTime.now()
		val initialDelay = 60L - now.second
		calendarView.requestedTime = now
		Globals.timerService.scheduleAtFixedRate({
			later {
				val dateTime = LocalDateTime.now()
				calendarView.today = dateTime.toLocalDate()
				calendarView.time = dateTime.toLocalTime()
			}
		}, initialDelay, 60L, TimeUnit.SECONDS)

		calendarView.setDefaultCalendarProvider { account.getDefaultCalendar() }
		rootTabPane.tabs.add(createAccountTab(calendarView, Icons.calendar(), account.nickNameProperty))
	}

	private fun addCardView(account : IContactAccount)
	{
		rootTabPane.tabs.add(createAccountTab(ContactView(account), Icons.book(), account.nickNameProperty))
	}

	private fun addMailView(account : IEmailAccount)
	{
		rootTabPane.tabs.add(createAccountTab(MailAccountView(account), Icons.mail(), account.nickNameProperty))
	}

	private val createAccountView =
		mapOf(IMAPAccount::class to { account : IAccount -> addMailView(account as IEmailAccount) },
			CalDAVAccount::class to { account -> addCalendarView(account as ICalendarAccount) },
			CardDAVAccount::class to { account -> addCardView(account as IContactAccount) })

	private fun saveUISettings()
	{
		val model = Globals.uiSettings.save(accounts.values)
		RDFDataMgr.write(
			Files.newOutputStream(
				Paths.get(
					OS.getSettingsDir(PinkPigMail::class.java).toString(), Globals.UI_FILE
				)
			), model, Lang.TURTLE
		)
	}

	//  shutdown sequence
	//	done on a thread, so the window closes immediately while shutdown goes ahead in the background
	private fun shutdown(@Suppress("UNUSED_PARAMETER") event : WindowEvent)
	{
		Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
		Thread {
			doAndIgnore { Globals.timerService.shutdown() }
			doAndIgnore { Globals.timerService.awaitTermination(10, TimeUnit.SECONDS) }
			doAndIgnore { Globals.service.shutdown() }
			doAndIgnore { Globals.service.awaitTermination(10, TimeUnit.SECONDS) }
			doAndIgnore(::saveUISettings)
			doAndIgnore(storage::close)
			exitProcess(1)
		}.start()
	}

	//	build the known accounts
	private fun buildAccounts()
	{
		storage.query(GET_ACCOUNTS).forEach {
			val aid = it.getResource("aid").toString()
			val account = ACCOUNT_CONSTRUCTORS[it.getResource("type").toString()]!!.invoke(aid)
			account.initialize()
			accounts[aid] = account
		}
	}

	//  boot sequence
	override fun start(stage : Stage)
	{
		//	set up an error handler for uncaught exceptions
		Thread.setDefaultUncaughtExceptionHandler(ErrorHandler())

		//	set up a stream handler to intercept URL loading -- needed to stop loading of images
		//	in mail messages for example
		//	TODO -- this should be done via some Service interface nowdays?
		//	and it should only be used in the mail html viewer (do they expose those features yet?)
		URL.setURLStreamHandlerFactory(CustomURLStreamHandlerFactory(Globals.htmlState))

		//	get the known accounts
		buildAccounts()

		//	initialize the stage
		with(stage) {
			scene = UIUtils.getScene(root)
			title = Strings.APP_NAME
			icons.add(Image(Icons.thePig128()))
			width = Globals.uiSettings.widthProperty.get()
			height = Globals.uiSettings.heightProperty.get()
			setOnCloseRequest(::shutdown)
		}

		with(Globals.uiSettings) {
			widthProperty.bind(stage.widthProperty())
			heightProperty.bind(stage.heightProperty())
		}

		//	create views for each account
		accounts.values.forEach { account -> createAccountView[account::class]?.let { it(account) } }

		//	add an event listener for storage events
		storage.addListener { event ->
			val infModel = JenaUtils.createRDFSModel(event, Vocabulary.eventSubClasses)
			//  TODO -- doesn't close the query model -- OK since in mem?
			QueryExecutionFactory.create(EVENT_IDS, infModel).execSelect().forEach {
				pushEvent(StorageEvent(it.get("eid"), it.get("type"), it.get("aid"), infModel))
			}
		}

		//	synch each account
		accounts.values.forEach { it.sync() }

		//  voodoo nonsense - see comments in subscribers -- should also probably do it with an actual JavaFX
		//  stage shown event
		later { Globals.fromModel.push(StageShowEvent()) }

		//	show the UI
		stage.show()
	}
}