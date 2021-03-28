package org.knowtiphy.pinkpigmail.model.events

import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.IFolder

open class AccountSyncStartedEvent(account : IAccount) : UIEvent(account)
{
}