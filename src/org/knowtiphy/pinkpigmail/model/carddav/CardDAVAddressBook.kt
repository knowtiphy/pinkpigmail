package org.knowtiphy.pinkpigmail.model.caldav

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer

class CardDAVAddressBook(accountId: String, storage: IStorage) : StoredPeer(accountId, storage)
{
    val cards: ObservableList<CardDAVCard> = FXCollections.observableArrayList()
    val groups: ObservableList<CardDAVGroup> = FXCollections.observableArrayList()
    
    val nameProperty = SimpleStringProperty()

    init
    {
        declareU(Vocabulary.HAS_NAME) { nameProperty.value = it.literal.string }
        declareOU(Vocabulary.CONTAINS, cards)
        declareD(Vocabulary.CONTAINS, cards)
        declareOU(Vocabulary.HAS_GROUP, groups)
        declareD(Vocabulary.HAS_GROUP, groups)
    }
}