package org.knowtiphy.pinkpigmail

import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.mailview.HTMLState
import org.knowtiphy.pinkpigmail.model.events.UIEvent
import org.knowtiphy.pinkpigmail.model.storage.StorageEvent
import org.knowtiphy.pinkpigmail.util.ui.UIUtils
import org.knowtiphy.utils.NameSource
import org.reactfx.EventSource
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

object Globals
{
	//  PPM style sheet location
	const val UI_FILE = "ui.ttl"

	//  PPM style sheet
	const val STYLE_SHEET = "/styles.css"

	//  single threaded executor for doing work on
	val service : ExecutorService = Executors.newSingleThreadExecutor()

	//	executor pool for doing periodic tasks like updating the current time in calendar views
	val timerService : ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

	//  name source for generating RDF ids/names
	val nameSource = NameSource(Vocabulary.NBASE)

	//  PPM UI settings
	val uiSettings : UISettings by lazy { UISettings.read(UI_FILE) }

	//	all storage related events are posted here
	//	only parts of the model should subscribe to these events
	val fromStorage = EventSource<StorageEvent>()

	//	all UI model related events are posted here
	//	only parts of the UI should subscribe to these events
	val fromModel = EventSource<UIEvent>()

	fun push(event : UIEvent)
	{
		//	by doing the later it puts the whole event stream on the FX UI thread
		UIUtils.later {
			//	push the event to the account's event stream
			if (event.account != null) event.account.events.push(event)
			//	push the event to the all events stream
			fromModel.push(event)
		}
	}

	//	voodoo nonsense to get around limitations in javafx WebView
	val htmlState = HTMLState()
}