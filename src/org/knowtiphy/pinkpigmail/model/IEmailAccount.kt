package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import org.knowtiphy.babbage.storage.StorageException
import org.knowtiphy.pinkpigmail.ITreeItem
import java.util.concurrent.ExecutionException

/**
 * @author graham
 */
interface IEmailAccount : ITreeItem, IAccount
{
    val inbox : IFolder
    val folders: ObservableList<IFolder>

    val isMoveDeletedMessagesToTrash: Boolean

    val isMoveJunkMessagesToJunk: Boolean

    val isCopySentMessagesToSent: Boolean

    val isDisplayMessageMarksAsRead: Boolean

    val serverNameProperty: StringProperty

    val emailAddressProperty: StringProperty

    val allowHTMLProperty: ReadOnlyBooleanProperty

    val trustedContentProviders: ObservableList<String>

    val trustedSenders: ObservableList<EmailAddress>

    fun trustSender(addresses: Collection<EmailAddress>)

    fun unTrustSender(addresses: Collection<EmailAddress>)

    fun isTrustedSender(addresses: Collection<EmailAddress>): Boolean

    fun trustProvider(url: String)

    fun unTrustProvider(url: String)

    fun isTrustedProvider(url: String): Boolean

    @Throws(StorageException::class, ExecutionException::class, InterruptedException::class)
    fun getReplyModel(message: IMessage, modelType: EmailModelType): IMessageModel

    fun getSendModel(modelType: EmailModelType): IMessageModel
}