package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import org.knowtiphy.pinkpigmail.ITreeItem
import org.knowtiphy.pinkpigmail.model.caldav.CardDAVAddressBook

interface ICardAccount : ITreeItem, IAccount
{
    val emailAddressProperty: StringProperty

    val addressBooks: ObservableList<CardDAVAddressBook>
}