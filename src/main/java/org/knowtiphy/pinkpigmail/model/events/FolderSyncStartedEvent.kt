package org.knowtiphy.pinkpigmail.model.events

import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.IFolder

class FolderSyncStartedEvent<T>(account : IAccount, folder : T) : FolderSyncEvent<T>(account, folder)
{
}