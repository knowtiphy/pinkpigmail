package org.knowtiphy.pinkpigmail.model.caldav

import javafx.beans.property.SimpleStringProperty
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.PPPeer

class CardDAVGroup(id: String, storage: IStorage) : PPPeer(id, storage)
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

