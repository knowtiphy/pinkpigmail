package org.knowtiphy.pinkpigmail

import org.knowtiphy.pinkpigmail.model.IAccount
import org.reactfx.EventSource

object Globals
{
	//	an account has been synched
	val synched = EventSource<IAccount>()
}