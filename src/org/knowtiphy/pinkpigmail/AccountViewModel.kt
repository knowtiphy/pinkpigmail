package org.knowtiphy.pinkpigmail

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.pinkpigmail.model.IMessage
import org.reactfx.EventSource

//  the view-model for the account view
class AccountViewModel(val mailAccount: IEmailAccount)
{
    private var currentFolder: IFolder? = null
    private val folderViewModels = HashMap<IFolder, FolderViewModel>()

    val visibleView: ObjectProperty<IFolder> = SimpleObjectProperty()

    //  event sources
    val folderSelected: EventSource<IFolder> = EventSource()
    val messagesSelected: EventSource<FolderViewModel> = EventSource()

    init
    {
        folderSelected.subscribe {
            currentFolder = it
            visibleView.set(it)
            messagesSelected.push(currentFolderViewModel())
        }
    }

    fun currentFolderViewModel(): FolderViewModel = folderViewModels[currentFolder!!]!!

    fun addFolderViewModel(folder: IFolder, model: FolderViewModel)
    {
        folderViewModels[folder] = model
    }

    fun currentMessages(): ObservableList<out IMessage>
    {
        assert(currentFolderViewModel().selectionModel!!.selectedItems.isNotEmpty())
        return currentFolderViewModel().selectionModel!!.selectedItems
    }

    fun currentMessage(): IMessage
    {
        assert(currentFolderViewModel().selectionModel!!.selectedItems.size == 1)
        return currentFolderViewModel().selectionModel!!.selectedItem
    }
}