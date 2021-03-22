package org.knowtiphy.pinkpigmail.model.events

import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.IFolder

class FolderSyncDoneEvent(account : IAccount, folder : IFolder) : FolderSyncEvent(account, folder)
{
}