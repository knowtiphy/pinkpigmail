package org.knowtiphy.pinkpigmail.cell

import javafx.scene.control.TreeCell
import org.knowtiphy.pinkpigmail.model.caldav.CardDAVAddressBook
import org.knowtiphy.pinkpigmail.model.caldav.CardDAVGroup

/**
 *
 * @author graham
 */
class AddressBookOrGroupCell : TreeCell<Any>()
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

    public override fun updateItem(item: Any?, empty: Boolean)
    {
        super.updateItem(item, empty)
        if (empty || item == null)
        {
            graphic = null
        } else
        {
            //  just in case we did the first case
//            graphic = box
            textProperty().bind(if (item is CardDAVAddressBook) item.nameProperty else ((item as CardDAVGroup)).nameProperty)
        }
    }
}