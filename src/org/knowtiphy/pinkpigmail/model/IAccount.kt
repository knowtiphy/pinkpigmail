package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.knowtiphy.pinkpigmail.ITreeItem
import org.knowtiphy.owlorm.javafx.IPeer
import org.knowtiphy.babbage.storage.StorageException
import java.util.concurrent.ExecutionException

/**
 * @author graham
 */
interface IAccount : ITreeItem, IAcc
{
    val folders: ObservableList<IFolder>

    val isMoveDeletedMessagesToTrash: Boolean

    val isMoveJunkMessagesToJunk: Boolean

    val isCopySentMessagesToSent: Boolean

    val isDisplayMessageMarksAsRead: Boolean

    val serverNameProperty: StringProperty

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
    fun getReplyModel(message: IMessage, modelType: ModelType): IMessageModel

    fun getSendModel(modelType: ModelType): IMessageModel
}