package org.knowtiphy.pinkpigmail

import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TableView
import org.knowtiphy.pinkpigmail.model.IMessage

//  the view-model for a folder view
class FolderViewModel
{
    //  the shared selection model for all perspectives within a folder view
    var selectionModel: TableView.TableViewSelectionModel<IMessage>? = null

    //  which of the perspectives in the folder view is visible
    val visiblePerspective = SimpleStringProperty()
}