package org.knowtiphy.pinkpigmail.model.imap

import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.Entity
import org.knowtiphy.pinkpigmail.model.IPart

/**
 * @author graham
 */
class IMAPPart(id: String, override val mimeType: String, override val content: String) : Entity(id, Vocabulary.IMAP_MESSAGE_PART), IPart
