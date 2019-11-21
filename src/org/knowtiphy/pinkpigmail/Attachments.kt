package org.knowtiphy.pinkpigmail

import javafx.collections.ObservableList
import javafx.scene.control.CustomMenuItem
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.layout.HBox
import org.knowtiphy.pinkpigmail.model.IAttachment
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.babbage.storage.StorageException
import org.knowtiphy.pinkpigmail.util.ActionHelper
import org.knowtiphy.pinkpigmail.util.ButtonHelper

import java.io.IOException

/**
 * @author graham
 */
class Attachments : HBox()
{
    companion object
    {
        @Throws(IOException::class, StorageException::class)
        fun addRemoveMenu(attachment: IAttachment, attachments: MutableList<IAttachment>, items: ObservableList<MenuItem>): CustomMenuItem
        {
            val item = CustomMenuItem()
            val nameLabel = Label(attachment.fileName)
            val removeButton = ButtonHelper.regular(ActionHelper.create(Icons.delete(Icons.SMALL_SIZE), {
                attachments.remove(attachment)
                items.remove(item)
            }, Strings.REMOVE_ATTACHMENT, false))
            val showButton = ButtonHelper.regular(ActionHelper.create(Mime.glyph(attachment.mimeType, Icons.SMALL_SIZE), { Actions.openAttachment(attachment) }, Strings.OPEN_ATTACHMENT,
                    !(Mime.isSafe(attachment.mimeType) || Mime.isSafeFileEnding(attachment.fileName))))

            val box = HBox(removeButton, showButton, nameLabel)
            box.spacing = 5.0

            item.content = box

            return item
        }

        //  this is ugly -- not very MVC
        @Throws(IOException::class, StorageException::class)
        fun addRemoveMenu(attachments: MutableList<IAttachment>, items: ObservableList<MenuItem>)
        {
            items.clear()
            for (attachment in attachments)
            {
                items.add(0, addRemoveMenu(attachment, attachments, items))
            }

            val removeAll = ButtonHelper.regular(ActionHelper.create(Icons.delete(Icons.SMALL_SIZE), {
                attachments.clear()
                items.clear()
            }, Strings.REMOVE_ALL_ATTACHMENTS, false))

            items.add(CustomMenuItem(removeAll))
        }

        @Throws(IOException::class, StorageException::class)
        fun viewSaveMenu(attachments: List<IAttachment>, items: ObservableList<MenuItem>)
        {
            for (attachment in attachments)
            {
                val saveButton = ButtonHelper.regular(ActionHelper.create(Icons.save(Icons.SMALL_SIZE),
                        { Actions.saveAttachment(attachment) }, Strings.SAVE_ATTACHMENT, false))

                val label = Label(attachment.fileName)

                val openButton = ButtonHelper.regular(
                        ActionHelper.create(Mime.glyph(attachment.mimeType, Icons.SMALL_SIZE),
                                { Actions.openAttachment(attachment) }, Strings.OPEN_ATTACHMENT,
                                !(Mime.isSafe(attachment.mimeType) || Mime.isSafeFileEnding(attachment.fileName))))

                val box = HBox(saveButton, openButton, label)
                box.spacing = 5.0
                val item = CustomMenuItem(box)
                items.add(item)
            }

            val saveAllButton = ButtonHelper.regular(ActionHelper.create(Icons.save(Icons.SMALL_SIZE), { Actions.saveAttachments(attachments) }, Strings.SAVE_ATTACHMENT, false))

            items.add(CustomMenuItem(saveAllButton))
        }
    }
}