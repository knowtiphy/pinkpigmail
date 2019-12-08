package org.knowtiphy.pinkpigmail.model

import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.owlorm.javafx.Peer

open class StoredPeer(id : String, val storage : IStorage) : Peer(id)
{
}