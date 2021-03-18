package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import org.knowtiphy.pinkpigmail.model.caldav.CardDAVAddressBook

interface IContactAccount : IAccount
{
    val emailAddressProperty: StringProperty

    val addressBooks: ObservableList<CardDAVAddressBook>
}