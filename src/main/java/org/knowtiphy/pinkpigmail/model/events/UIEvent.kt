package org.knowtiphy.pinkpigmail.model.events

import org.knowtiphy.pinkpigmail.model.IAccount
import kotlin.reflect.KClass

open class UIEvent(val account : IAccount?)
{
	constructor() : this(null)

	fun isA(cls : Class<*>) : Boolean
	{
		//	TODO -- is this how you test class equality in Kotlin?
		return this::class == cls
	}
}