package org.knowtiphy.pinkpigmail.model.caldav

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer

class CardDAVAddressBook(accountId: String, storage: IStorage) : StoredPeer(accountId, Vocabulary.CARDDAV_ADDRESSBOOK, storage)
{
    val nameProperty = SimpleStringProperty()

    val cards: ObservableList<CardDAVCard> = FXCollections.observableArrayList()
    val groups: ObservableList<CardDAVGroup> = FXCollections.observableArrayList()

    init
    {
        addUpdater(Vocabulary.HAS_NAME, nameProperty)
//        declareOU(Vocabulary.CONTAINS, cards)
//        declareD(Vocabulary.CONTAINS, cards)
//        declareOU(Vocabulary.HAS_GROUP, groups)
//        declareD(Vocabulary.HAS_GROUP, groups)
    }
}