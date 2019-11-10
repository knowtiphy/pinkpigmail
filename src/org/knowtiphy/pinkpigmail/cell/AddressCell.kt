package org.knowtiphy.pinkpigmail.cell

import javafx.beans.binding.Bindings
import javafx.collections.ObservableList
import org.knowtiphy.pinkpigmail.model.EmailAddress
import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.IMessage
import java.util.concurrent.Callable

/**
 * @author graham
 */
class AddressCell(private val account: IAccount,
                  private val extractor: (IMessage) -> ObservableList<EmailAddress>) : BaseCell()
{
    public override fun updateItem(item: IMessage?, empty: Boolean)
    {
        super.updateItem(item, empty)
        removePrevious()
        graphic = null
        if (empty || item == null)
        {
            text = null
        } else
        {
            val address = extractor.invoke(item)
            textProperty().bind(Bindings.createStringBinding(
                    Callable<String> { EmailAddress.format(account, address) }, address, account.trustedSenders))
        }
    }
}
