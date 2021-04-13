package org.knowtiphy.pinkpigmail.model.events

import org.knowtiphy.pinkpigmail.model.IAccount
import kotlin.reflect.KClass

open class UIEvent(val account : IAccount?)
{
	constructor() : this(null)
}