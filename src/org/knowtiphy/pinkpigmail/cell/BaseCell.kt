package org.knowtiphy.pinkpigmail.cell

import javafx.scene.control.TableCell
import org.knowtiphy.pinkpigmail.model.IMessage

/**
 *
 * @author graham
 */
open class BaseCell : TableCell<IMessage, IMessage>()
{
    protected open fun removePrevious()
    {
        textProperty().unbind()
    }
}
