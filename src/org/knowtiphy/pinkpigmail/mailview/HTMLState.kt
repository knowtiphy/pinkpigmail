package org.knowtiphy.pinkpigmail.mailview

import org.knowtiphy.pinkpigmail.model.IMessage

/**
 *
 * @author graham
 */
class HTMLState
{
    //  this is a hack, but forced on us by the way JavaFX WebView doesn't expose handlers
    var message: IMessage? = null
    var isAllowJars = true
}
