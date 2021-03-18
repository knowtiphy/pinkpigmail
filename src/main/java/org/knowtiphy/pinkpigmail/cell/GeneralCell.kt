package org.knowtiphy.pinkpigmail.cell

import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableValue
import org.knowtiphy.pinkpigmail.model.IMessage
import java.util.concurrent.Callable

/**
 *
 * @author graham
 */
open class GeneralCell<T, E : ObservableValue<T>>(
        private val extractor: (IMessage) -> E, private val stringify: (E) -> String) : BaseCell()
{
    public override fun updateItem(item: IMessage?, empty: Boolean)
    {
        super.updateItem(item, empty)
        removePrevious()
        graphic = null
        if (empty || item == null)
        {
            setText(null)
        } else
        {
            val value = extractor.invoke(item)
            textProperty().bind(Bindings.createStringBinding(Callable<String> { stringify.invoke(value) }, value))
        }
    }
}
