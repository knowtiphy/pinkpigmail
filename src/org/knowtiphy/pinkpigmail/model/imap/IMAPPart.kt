package org.knowtiphy.pinkpigmail.model.imap

import org.knowtiphy.owlorm.javafx.Entity
import org.knowtiphy.pinkpigmail.model.IPart
import org.knowtiphy.babbage.storage.IStorage

/**
 * @author graham
 */
class IMAPPart(id: String, storage: IStorage, override val mimeType: String, override val content: String) : Entity(id), IPart
