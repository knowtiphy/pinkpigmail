package org.knowtiphy.pinkpigmail.model.caldav

//  life cycle of events top to bottom
enum class EventState
{
	NEW,
	EDITING,
	SAVED,
	BABBAGE,
}