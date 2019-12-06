package org.knowtiphy.pinkpigmail

import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.knowtiphy.pinkpigmail.cell.CardCell
import org.knowtiphy.pinkpigmail.model.ICardAccount
import org.knowtiphy.pinkpigmail.model.caldav.CardDAVAddressBook
import org.knowtiphy.pinkpigmail.model.caldav.CardDAVCard
import org.knowtiphy.pinkpigmail.model.caldav.CardDAVGroup
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.ActionHelper
import org.knowtiphy.pinkpigmail.util.ButtonHelper
import org.knowtiphy.pinkpigmail.util.MappedFlipper
import org.knowtiphy.pinkpigmail.util.UIUtils

class ContactView(account: ICardAccount) : VBox()
{
    private val groupsRoot = TreeItem<Any>()

    init
    {
        val pad = AccountViewModel<ICardAccount, CardDAVAddressBook, CardDAVCard>(account)

        val accountView = createAccountView(pad)
        val toolBar = createToolBar(pad)

        val folderViews = MappedFlipper<CardDAVAddressBook>(pad.currentEntityProperty)
        UIUtils.resizable(folderViews)

        val folderArea = SplitPane(accountView, folderViews)
        UIUtils.resizable(folderArea)

        //  when an account folder is added to the accounts folder list add an item to the account view
        //  and add a folder view for the folder

        account.addressBooks.addListener { c: ListChangeListener.Change<out CardDAVAddressBook> ->
            while (c.next())
            {
                c.addedSubList.forEach { addressBook ->
                    val treeItem = TreeItem<Any>(addressBook)
                    groupsRoot.children.add(treeItem)
                    addressBook.groups.addListener { c: ListChangeListener.Change<out CardDAVGroup> ->
                        while (c.next())
                        {
                            c.addedSubList.forEach { group ->
                                val treeItem = TreeItem<Any>(group)
                                groupsRoot.children.add(treeItem)
//                                val fvm = CategoryViewModel<CardDAVAddressBook, CardDAVCard>(folder)
//                                pad.addCategoryViewModel(folder, fvm)
//                                val flipper = createFolderView(fvm)
//                                //  publish events -- new message selected
//                                EventStreams.changesOf((fvm.selectionModel
//                                        ?: return@forEach).selectedIndices).subscribe { pad.entitySelected.push(fvm) }
//                                folderViews.addNode(folder, flipper)
//                                println(folderViews.nodes)
                            }
                        }
                    }
//                    val fvm = CategoryViewModel<CardDAVAddressBook, CardDAVCard>(folder)
//                    pad.addCategoryViewModel(folder, fvm)
//                    val flipper = createFolderView(fvm)
//                    //  publish events -- new message selected
//                    EventStreams.changesOf((fvm.selectionModel
//                            ?: return@forEach).selectedIndices).subscribe { pad.entitySelected.push(fvm) }
//                    folderViews.addNode(folder, flipper)
//                    println(folderViews.nodes)
                }
            }
        }

        children.addAll(toolBar, folderArea)
        setVgrow(toolBar, Priority.NEVER)
        setVgrow(folderArea, Priority.ALWAYS)

        folderArea.setDividerPositions(PinkPigMail.uiSettings.verticalPosition[0].position)

//        //  TODO is this is necessary to try to work around a JavaFX bug with setting split pane positions?
//        primaryStage.setOnShown {
//            Platform.runLater {
//                folderArea.setDividerPositions(PinkPigMail.uiSettings.verticalPosition[0].position)
//                Bindings.bindContent<SplitPane.Divider>(PinkPigMail.uiSettings.verticalPosition, folderArea.dividers)
//            }
//        }
    }

    private fun createToolBar(pad: AccountViewModel<ICardAccount, CardDAVAddressBook, CardDAVCard>): Node
    {
        val delete = ActionHelper.create(Icons.delete(),
                {
                    //  move to the next message
                    val indices = pad.currentCategoryViewModel().selectionModel!!.selectedIndices
                   // Actions.deleteMessages(pad.currentEntities())
                    pad.currentCategoryViewModel().selectionModel!!.clearAndSelect(if (indices.isEmpty()) 0 else indices[indices.size - 1] + 1)
                }, Strings.DELETE)

        // val singleMessageActions = arrayOf()//reply, replyAll, forward)
        val multiMessageActions = arrayOf(delete)///, markJunk, markNotJunk)

        val toolBar = HBox()

        toolBar.children.addAll(UIUtils.spacer(), ButtonHelper.regular(delete), UIUtils.spacer())
        toolBar.padding = Insets(1.0, 0.0, 1.0, 0.0)
        toolBar.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)

        setVgrow(toolBar, Priority.NEVER)

        pad.entitySelected.subscribe { fvm ->
            //singleMessageActions.forEach { it.disabledProperty().set(fvm.selectionModel!!.selectedIndices.size != 1) }
            multiMessageActions.forEach { it.disabledProperty().set(fvm.selectionModel!!.selectedIndices.isEmpty()) }
        }

        return toolBar
    }

    private fun createAccountView(pad: AccountViewModel<ICardAccount, CardDAVAddressBook, CardDAVCard>): TreeView<Any>
    {
        val view = TreeView(groupsRoot)
        UIUtils.resizable(view)
        view.isShowRoot = false
//        view.setCellFactory { FolderCell() }

        //  publish events -- new folder selected (single selection model)
//        EventStreams.changesOf(view.selectionModel.selectedItems)
//                .filter { it.list.size == 1 }
//                .subscribe { accountViewModel.currentEntityProperty.set(it.list[0].value) }

        return view
//        val view = ListView<CardDAVAddressBook>(pad.account.addressBooks)
//        view.setCellFactory { AddressBookCell() }
//        UIUtils.resizable(view)
//
//        //  publish events -- new folder selected (single selection model)
//        EventStreams.changesOf(view.selectionModel.selectedItems)
//                .filter { it.list.size == 1 }
//                .subscribe { pad.currentEntityProperty.set(it.list[0]) }
//
//        return view
    }

    private fun createFolderView(fvm: CategoryViewModel<CardDAVAddressBook, CardDAVCard>): SplitPane//MappedFlipper<CardDAVCard>
    {
        val hPerspective = createCardPerspective(fvm)
        UIUtils.resizable(hPerspective)
        return hPerspective
    }

    private fun createCardPerspective(fvm: CategoryViewModel<CardDAVAddressBook, CardDAVCard>): SplitPane//MappedFlipper<CardDAVCard>
    {
        val left = ListView<CardDAVCard>(fvm.category.cards)
        left.setCellFactory { CardCell() }
        UIUtils.resizable(left)

        fvm.selectionModel = left.selectionModel

        val right = VBox()//createPerAccountHeaderView(fvm, folder, messageView)
        UIUtils.resizable(right)

        right.children.add(Label("RIGHT"))

        val splitPane = SplitPane(left, right)
        splitPane.orientation = Orientation.HORIZONTAL
        UIUtils.resizable(splitPane)

        //val flipper = MappedFlipper<CardDAVCard>()

        return splitPane
    }
}