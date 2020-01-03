package org.knowtiphy.pinkpigmail

import org.knowtiphy.pinkpigmail.mailview.HTMLState
import org.knowtiphy.pinkpigmail.model.IAccount
import org.reactfx.EventSource
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

object Globals
{
	//	an account has been synched
	val synched = EventSource<IAccount>()

	val htmlState = HTMLState()

	//	executor pool for doing periodic tasks like updating the current time in calendar views
	val timerService : ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
}