package org.knowtiphy.pinkpigmail.model.events

import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.IFolder

open class StartSyncEvent<T>(val synced : T, account : IAccount) : UIEvent(account)
{}