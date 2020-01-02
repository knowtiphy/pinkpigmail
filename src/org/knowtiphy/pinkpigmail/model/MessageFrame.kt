package org.knowtiphy.pinkpigmail.model

//	a message frame is a message together with it's surrounding messages (grouped into collections of messages)

class MessageFrame(val message: IMessage, val surroundingMessages: Collection<Collection<IMessage>>)
{
	fun loadAhead()
	{
		println("STARTING LOADHEAD :: " + message);
		surroundingMessages.forEach(message.folder::loadAhead)
	}
}