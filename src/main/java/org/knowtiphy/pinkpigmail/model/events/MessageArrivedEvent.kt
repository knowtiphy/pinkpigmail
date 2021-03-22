package org.knowtiphy.pinkpigmail.model.events

import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.IFolder

class MessageArrivedEvent(account : IAccount, val folder : IFolder) : UIEvent(account)
{
}