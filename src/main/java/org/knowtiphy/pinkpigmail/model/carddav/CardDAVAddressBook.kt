package org.knowtiphy.pinkpigmail.model.caldav

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.model.storage.DavStorage

class CardDAVAddressBook(accountId: String, storage: DavStorage) : StoredPeer<DavStorage>(accountId, storage)
{
    val nameProperty = SimpleStringProperty()

    val cards: ObservableList<CardDAVCard> = FXCollections.observableArrayList()
    val groups: ObservableList<CardDAVGroup> = FXCollections.observableArrayList()

    init
    {
        declareU(Vocabulary.HAS_NAME) { nameProperty.value = it.literal.string }
        declareOU(Vocabulary.CONTAINS, cards)
        declareD(Vocabulary.CONTAINS, cards)
        declareOU(Vocabulary.HAS_GROUP, groups)
        declareD(Vocabulary.HAS_GROUP, groups)
    }
}