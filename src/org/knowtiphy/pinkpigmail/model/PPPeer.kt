package org.knowtiphy.pinkpigmail.model

import org.knowtiphy.owlorm.javafx.Peer
import org.knowtiphy.babbage.storage.IStorage

open class PPPeer(id : String, val storage : IStorage) : Peer(id)
{
}