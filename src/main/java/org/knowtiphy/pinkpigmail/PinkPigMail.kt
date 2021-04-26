package org.knowtiphy.pinkpigmail

import com.calendarfx.view.CalendarView
import com.calendarfx.view.DateControl
import javafx.application.Application
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.stage.WindowEvent
import javafx.util.Callback
import org.apache.jena.arq.querybuilder.SelectBuilder
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.controlsfx.glyphfont.Glyph
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.StorageFactory
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.calendarview.Popover
import org.knowtiphy.pinkpigmail.mailaccountview.MailAccountView
import org.knowtiphy.pinkpigmail.mailview.CustomURLStreamHandlerFactory
import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.ICalendarAccount
import org.knowtiphy.pinkpigmail.model.IContactAccount
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.caldav.CalDAVAccount
import org.knowtiphy.pinkpigmail.model.caldav.CalDavEventFactory
import org.knowtiphy.pinkpigmail.model.caldav.CardDAVAccount
import org.knowtiphy.pinkpigmail.model.caldav.EventState
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
		//  @formatter:off
		private val ACCOUNT_CONSTRUCTORS = mapOf<String, (String) -> IAccount>(
			Vocabulary.IMAP_ACCOUNT to { uri : String -> IMAPAccount(uri, storage) },
			Vocabulary.CALDAV_ACCOUNT to { uri -> CalDAVAccount(uri, storage) },
			Vocabulary.CARDDAV_ACCOUNT to { uri -> CardDAVAccount(uri, storage) }
		)
		//  @formatter:on
	}

	//  @formatter:off
	private val createAccountView = mapOf(
		IMAPAccount::class to { account : IAccount -> addMailView(account as IEmailAccount) },
		CalDAVAccount::class to { account -> addCalendarView(account as ICalendarAccount) },
		CardDAVAccount::class to { account -> addCardView(account as IContactAccount) }
	)
	//  @formatter:on

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

	private fun showEntryDetails(param : DateControl.EntryDetailsParameter) : Boolean
	{
		val saved = Popover().call(param)
		if (!saved && param.entry.userObject == EventState.NEW) param.entry.calendar.removeEntry(param.entry)
		return saved
	}

	private fun addCalendarView(account : ICalendarAccount)
	{
		val calendarView = CalendarView()
		calendarView.calendarSources.add(account.source)

		calendarView.entryDetailsCallback = Callback { param ->
			val evt = param.inputEvent
			//  this one for double clicking an entry
			if (evt is MouseEvent)
			{
				if (evt.clickCount == 2) showEntryDetails(param) else false
			}
			//  this one for things like invoking from a menu
			else
			{
				showEntryDetails(param)
			}
		}

		val now = LocalTime.now()
		val initialDelay = 60L - now.second
		calendarView.requestedTime = now
		calendarView.entryFactory = CalDavEventFactory(storage)
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

	private fun saveUISettings()
	{
		val model = Globals.uiSettings.save(accounts.values)
		RDFDataMgr.write(
			Files.newOutputStream(
				Paths.get(OS.getSettingsDir(PinkPigMail::class.java).toString(), Globals.UI_FILE)
			), model, Lang.TURTLE
		)
	}

	//  shutdown sequence
	//	done on a thread, so the window closes immediately while shutdown goes ahead in the background
	@Suppress("UNUSED_PARAMETER")
	private fun shutdown(event : WindowEvent)
	{
		Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
		Thread {
			doAndIgnore(storage::close)
			doAndIgnore { Globals.timerService.shutdown() }
			doAndIgnore { Globals.timerService.awaitTermination(10, TimeUnit.SECONDS) }
			doAndIgnore { Globals.service.shutdown() }
			doAndIgnore { Globals.service.awaitTermination(10, TimeUnit.SECONDS) }
			doAndIgnore(::saveUISettings)
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

		stage.show()
	}
}

//	private fun showEntryDetails(control : DateControl, entry : Entry<*>, owner : Node, screenY : Double)
//	{
//		val contentCallback : Callback<EntryDetailsPopOverContentParameter, Node> =
//			control.entryDetailsPopOverContentCallback
//		//checkNotNull(contentCallback) { "No content callback found for entry popover" }
//		val entryPopOver = PopOver()
//		val param = EntryDetailsPopOverContentParameter(entryPopOver, control, owner, entry)
//		var content : Node? = contentCallback.call(param) as Node
//		if (content == null)
//		{
//			content = Label(Messages.getString("DateControl.NO_CONTENT"))
//		}
//		entryPopOver.contentNode = content
//		val location = ViewHelper.findPopOverArrowLocation(owner)
//		entryPopOver.arrowLocation = location
//		val position = ViewHelper.findPopOverArrowPosition(owner, screenY, entryPopOver.arrowSize, location)
//		entry.userObject = EventState.EDITING
//		entryPopOver.onHidden = EventHandler<WindowEvent> { println("CLOSED")}
//		entryPopOver.show(owner, position.x, position.y)
//	}
//
//	fun showDateDetails(control : DateControl, owner : Node, date : LocalDate)
//	{
//		val datePopOver : PopOver = DatePopOver(control, date)
//		datePopOver.show(owner)
//	}
//		calendarView.entryDetailsPopOverContentCallback =
//			Callback<EntryDetailsPopOverContentParameter, Node> { param : EntryDetailsPopOverContentParameter ->
//				val foo = EntryPopOverContentPane(param.popOver, param.dateControl, param.entry)
////				foo.bottom = Label("XXXX")
//				VBox(foo, Label("XXX"))
//			}

//		calendarView.entryDetailsCallback = Callback<DateControl.EntryDetailsParameter, Boolean> { param ->
//			val evt : InputEvent = param.inputEvent
//			if (evt is MouseEvent)
//			{
//				if (evt.clickCount == 2)
//				{
//					this.showEntryDetails(param.dateControl, param.entry, param.owner, param.screenY)
//					true
//				} else
//				{
//					false
//				}
//			} else
//			{
//				println("XXXX " + evt)
//				this.showEntryDetails(param.dateControl, param.entry, param.owner, param.screenY)
//				true
//			}
//		}
