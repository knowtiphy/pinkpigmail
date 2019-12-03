package org.knowtiphy.pinkpigmail

import javafx.scene.control.MultipleSelectionModel

//  the view-model for a category view
class CategoryViewModel<C, E>(val category: C)
{
    //  the shared selection model for all perspectives within a category view
    var selectionModel: MultipleSelectionModel<E>? = null
}