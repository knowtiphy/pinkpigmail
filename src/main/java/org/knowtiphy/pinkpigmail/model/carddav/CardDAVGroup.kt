package org.knowtiphy.pinkpigmail.model.caldav

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer

class CardDAVGroup(id: String, storage: IStorage) : StoredPeer(id, Vocabulary.CARDDAV_GROUP, storage)
{
	val nameProperty = SimpleStringProperty()
	val cards = FXCollections.observableArrayList<CardDAVCard>()

	init
	{
		addUpdater(Vocabulary.HAS_NAME, nameProperty)
//		declareOU(Vocabulary.HAS_CARD, cards)
//		declareD(Vocabulary.HAS_CARD, cards)
	}
}

