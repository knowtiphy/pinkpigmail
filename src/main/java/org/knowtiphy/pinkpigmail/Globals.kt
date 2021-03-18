package org.knowtiphy.pinkpigmail

import org.knowtiphy.pinkpigmail.mailview.HTMLState
import org.knowtiphy.pinkpigmail.util.ui.StorageEvent
import org.reactfx.EventSource
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

object Globals
{
	//	event source for all storage events
	val events = EventSource<StorageEvent>()

	val htmlState = HTMLState()

	//	executor pool for doing periodic tasks like updating the current time in calendar views
	val timerService : ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
}