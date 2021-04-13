package org.knowtiphy.pinkpigmail.cell

import javafx.geometry.Pos
import javafx.scene.layout.HBox
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.resources.Icons

/**
 *
 * @author graham
 */
class StatusCell : BaseCell()
{
    private val junk = Icons.junk()
    private val unread = Icons.unread()
    private val answered = Icons.answered()
    private val deleted = Icons.deleted()

    private val box = HBox(junk, unread, answered, deleted)

    init
    {
        box.spacing = 2.0
        box.alignment = Pos.CENTER_LEFT
        junk.managedProperty().bind(junk.visibleProperty())
        unread.managedProperty().bind(unread.visibleProperty())
        answered.managedProperty().bind(answered.visibleProperty())
        deleted.managedProperty().bind(deleted.visibleProperty())
    }

    override fun removePrevious()
    {
        junk.visibleProperty().unbind()
        unread.visibleProperty().unbind()
        answered.visibleProperty().unbind()
        deleted.visibleProperty().unbind()
        if (tableRow != null)
        {
            tableRow.disableProperty().unbind()
        }
    }

    public override fun updateItem(item: IMessage?, empty: Boolean)
    {
        super.updateItem(item, empty)
        removePrevious()
        text = null
        if (empty || item == null)
        {
            graphic = null
        } else
        {
            //  just in case we did the first case
            graphic = box
            junk.visibleProperty().bind(item.junkProperty)
            unread.visibleProperty().bind(item.readProperty.not())
            answered.visibleProperty().bind(item.answeredProperty)
            deleted.visibleProperty().bind(item.disabledProperty)
            tableRow.disableProperty().bind(item.disabledProperty)
        }
    }
}