package org.knowtiphy.pinkpigmail.cell

import javafx.scene.control.ListCell
import org.knowtiphy.pinkpigmail.model.caldav.CardDAVCard

/**
 *
 * @author graham
 */
class CardCell : ListCell<CardDAVCard>()
{
//    private val junk = Icons.junk()
//    private val unread = Icons.unread()
//    private val answered = Icons.answered()
//    private val deleted = Icons.deleted()

//    init
//    {
//        box.spacing = 2.0
//        box.alignment = Pos.CENTER_LEFT
//        junk.managedProperty().bind(junk.visibleProperty())
//        unread.managedProperty().bind(unread.visibleProperty())
//        answered.managedProperty().bind(answered.visibleProperty())
//        deleted.managedProperty().bind(deleted.visibleProperty())
//    }

    public override fun updateItem(item: CardDAVCard?, empty: Boolean)
    {
        super.updateItem(item, empty)
//        text = null
        if (empty || item == null)
        {
            graphic = null
           // text = null
        } else
        {
            //  just in case we did the first case
//            graphic = box
            textProperty().bind(item.hasNameProperty)
        }
    }
}