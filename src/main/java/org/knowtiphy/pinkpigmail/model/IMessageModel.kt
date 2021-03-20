package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import org.knowtiphy.babbage.storage.exceptions.StorageException

import java.io.IOException

/**
 * @author graham
 */
interface IMessageModel
{
    val mailAccount: IEmailAccount

    val copyTo: IFolder

    val sendMode: EmailSendMode

    val attachments: ObservableList<IAttachment>

    @Throws(StorageException::class, IOException::class)
    fun send()

    @Throws(StorageException::class)
    fun save()

    @Throws(StorageException::class)
    fun saveToDrafts()

    fun subjectProperty(): StringProperty

    fun toProperty(): StringProperty

    fun ccProperty(): StringProperty

    fun contentProperty(): StringProperty

    fun addAttachment(attachment: IAttachment)
}
