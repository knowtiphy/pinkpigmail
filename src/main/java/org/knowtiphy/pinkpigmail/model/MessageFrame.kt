package org.knowtiphy.pinkpigmail.model

//	a message frame is a message together with it's surrounding messages (grouped into collections of messages)

class MessageFrame(private val message: IMessage, private val surroundingMessages: Collection<Collection<IMessage>>)
{
	//fun loadAhead() = { println("LA " +surroundingMessages); surroundingMessages.forEach(message.folder::loadAhead)
}