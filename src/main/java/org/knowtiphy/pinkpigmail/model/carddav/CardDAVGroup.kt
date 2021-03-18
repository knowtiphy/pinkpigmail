package org.knowtiphy.pinkpigmail.model.caldav

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.model.storage.DavStorage

class CardDAVGroup(id: String, storage: DavStorage) : StoredPeer<DavStorage>(id, storage)
{
	val nameProperty = SimpleStringProperty()
	val cards = FXCollections.observableArrayList<CardDAVCard>()

	init
	{
		declareU(Vocabulary.HAS_NAME, nameProperty)
		declareOU(Vocabulary.HAS_CARD, cards)
		declareD(Vocabulary.HAS_CARD, cards)
	}
}

