package org.knowtiphy.pinkpigmail.model.events

import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.IFolder

open class FolderSyncEvent(account : IAccount, val folder : IFolder) : UIEvent(account)
{
}