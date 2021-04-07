package org.knowtiphy.pinkpigmail.model

import org.knowtiphy.pinkpigmail.model.imap.IHasMimeType

/**
 * @author graham
 */
interface IPart : IHasMimeType
{
 //   val mimeType: String

    val content: String
}
