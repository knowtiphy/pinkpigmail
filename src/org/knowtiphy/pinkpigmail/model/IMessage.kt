package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import org.knowtiphy.owlorm.javafx.IPeer
import org.knowtiphy.pinkpigmail.model.imap.IMAPCIDPart
import java.net.URL
import java.time.ZonedDateTime
import java.util.concurrent.Future

/**
 * @author graham
 */
interface IMessage : IPeer
{
    val mailAccount: IEmailAccount

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

    val sentOnProperty: ObjectProperty<ZonedDateTime>

    val receivedOnProperty: ObjectProperty<ZonedDateTime>

    val loadRemoteProperty: BooleanProperty

    fun loadAhead() : Future<*>?

    fun getContent(allowHTML: Boolean): IPart
}