package org.knowtiphy.pinkpigmail.model.caldav

import javafx.beans.property.SimpleStringProperty
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.model.storage.DavStorage

class CardDAVCard(id: String, storage: DavStorage) : StoredPeer<DavStorage>(id, storage)
{
   val hasNameProperty = SimpleStringProperty()
    //, optional: 0+ telephone numbers (each with a type), 0+ emails (each with a type)

    init
    {
        declareU(Vocabulary.HAS_NAME, hasNameProperty)
//        declareU(Vocabulary.HAS_DATE_START, ::setStartDate)
//        declareU(Vocabulary.HAS_DATE_END, ::setEndDate)
    }
}

