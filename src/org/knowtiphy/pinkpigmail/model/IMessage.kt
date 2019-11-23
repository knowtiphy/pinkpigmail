package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import org.knowtiphy.pinkpigmail.model.imap.IMAPCIDPart
import org.knowtiphy.owlorm.javafx.IPeer
import org.knowtiphy.babbage.storage.StorageException
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.Future

/**
 * @author graham
 */
interface IMessage : IPeer
{
    val mailAccount: IMailAccount

    val folder: IFolder

    val from: ObservableList<EmailAddress>

    val to: ObservableList<EmailAddress>

    val cc: ObservableList<EmailAddress>

    val bcc: ObservableList<EmailAddress>

    val attachments: ObservableList<IAttachment>

    val isHTML: Boolean

    val cidMap: Map<URL, IMAPCIDPart>

    val readProperty: BooleanProperty

    val answeredProperty: BooleanProperty

    val junkProperty: BooleanProperty

    val subjectProperty: StringProperty

    val sentOnProperty: ObjectProperty<LocalDate>

    val receivedOnProperty: ObjectProperty<LocalDate>

    val loadRemoteProperty: BooleanProperty

    fun loadAhead(): Future<*>

    fun setFuture(future: Future<*>)

    @Throws(StorageException::class)
    fun getContent(allowHTML: Boolean): IPart
}