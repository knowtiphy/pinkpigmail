package org.knowtiphy.pinkpigmail

import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.knowtiphy.pinkpigmail.cell.AddressBookOrGroupCell
import org.knowtiphy.pinkpigmail.cell.CardCell
import org.knowtiphy.pinkpigmail.model.IContactAccount
import org.knowtiphy.pinkpigmail.model.caldav.CardDAVAddressBook
import org.knowtiphy.pinkpigmail.model.caldav.CardDAVCard
import org.knowtiphy.pinkpigmail.model.caldav.CardDAVGroup
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.maxSizeable
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.resizeable

class ContactView(account: IContactAccount) : VBox()
{
    private val groupsRoot = TreeItem<Any>()

    init
    {
        val pad = AccountViewModel<IContactAccount, CardDAVAddressBook, CardDAVCard>(account)

        val accountView = createAccountView(pad)
        val toolBar = createToolBar(pad)

       // val folderViews = resizeable(MappedReplacer<TreeItem<CardDAVAddressBook>>(pad.selectedCategory))

       // val folderArea = resizeable(SplitPane(accountView, folderViews))

        //  when an account folder is added to the accounts folder list add an item to the account view
        //  and add a folder view for the folder

        account.addressBooks.addListener { c: ListChangeListener.Change<out CardDAVAddressBook> ->
            while (c.next())
            {
                c.addedSubList.forEach { addressBook ->
                    val addressBookItem = TreeItem<Any>(addressBook)
                    addressBookItem.isExpanded = true
                    groupsRoot.children.add(addressBookItem)
                    addressBook.groups.addListener { c: ListChangeListener.Change<out CardDAVGroup> ->
                        while (c.next())
                        {
                            c.addedSubList.forEach { group ->
                                val groupItem = TreeItem<Any>(group)
                                addressBookItem.children.add(groupItem)
//                                val fvm = CategoryViewModel<CardDAVGroup, CardDAVCard>(group)
//                                pad.addCategoryViewModel(group, fvm)
//                                val flipper = createFolderView(fvm)
//                                //  publish events -- new message selected
//                                EventStreams.changesOf((fvm.selectionModel
//                                        ?: return@forEach).selectedIndices).subscribe { pad.entitySelected.push(fvm) }
//                                folderViews.addNode(addressBook, flipper)
//                                println(folderViews.nodes)
                            }
                        }
                    }
                }
            }
        }

//        children.addAll(toolBar, folderArea)
//        setVgrow(toolBar, Priority.NEVER)
//        setVgrow(folderArea, Priority.ALWAYS)
//
//        folderArea.setDividerPositions(PinkPigMail.uiSettings.verticalPosition[0].position)

//        //  TODO is this is necessary to try to work around a JavaFX bug with setting split pane positions?
//        primaryStage.setOnShown {
//            Platform.runLater {
//                folderArea.setDividerPositions(PinkPigMail.uiSettings.verticalPosition[0].position)
//                Bindings.bindContent<SplitPane.Divider>(PinkPigMail.uiSettings.verticalPosition, folderArea.dividers)
//            }
//        }
    }

    private fun createToolBar(pad: AccountViewModel<IContactAccount, CardDAVAddressBook, CardDAVCard>): Node
    {
//        val delete = ActionHelper.create(Icons.delete(),
//                {
//                    //  move to the next message
//                    val indices = pad.currentCategoryViewModel().getSelectionModel()!!.selectedIndices
//                   // Actions.deleteMessages(pad.currentEntities())
//                    pad.currentCategoryViewModel().getSelectionModel()!!.clearAndSelect(if (indices.isEmpty()) 0 else indices[indices.size - 1] + 1)
//                }, Strings.DELETE)

        // val singleMessageActions = arrayOf()//reply, replyAll, forward)
//        val multiMessageActions = arrayOf(delete)///, markJunk, markNotJunk)

        val toolBar = maxSizeable(HBox())

//        toolBar.children.addAll(UIUtils.hSpacer(), ButtonHelper.regular(delete), UIUtils.hSpacer())
        toolBar.padding = Insets(1.0, 0.0, 1.0, 0.0)
//        toolBar.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)

        setVgrow(toolBar, Priority.NEVER)

//        pad.entitySelected.subscribe { fvm ->
//            //singleMessageActions.forEach { it.disabledProperty().set(fvm.selectionModel!!.selectedIndices.size != 1) }
//            multiMessageActions.forEach { it.disabledProperty().set(fvm.selectionModel!!.selectedIndices.isEmpty()) }
//        }

        return toolBar
    }

    private fun createAccountView(pad: AccountViewModel<IContactAccount, CardDAVAddressBook, CardDAVCard>): TreeView<Any>
    {
        val view = resizeable(TreeView(groupsRoot))
        view.isShowRoot = false
        view.setCellFactory { AddressBookOrGroupCell() }

        //  publish events -- new folder selected (single selection model)
//        EventStreams.changesOf(view.selectionModel.selectedItems)
//                .filter { it.list.size == 1 }
//                .subscribe { accountViewModel.currentEntityProperty.set(it.list[0].value) }

        return view
    }

    private fun createFolderView(fvm: CategoryViewModel<CardDAVAddressBook, CardDAVCard>): SplitPane//MappedFlipper<CardDAVCard>
    {
        return resizeable(createCardPerspective(fvm))
    }

    private fun createCardPerspective(fvm: CategoryViewModel<CardDAVAddressBook, CardDAVCard>): SplitPane//MappedFlipper<CardDAVCard>
    {
        val left = resizeable(ListView<CardDAVCard>(fvm.category.cards))
        left.setCellFactory { CardCell() }

      //  fvm.setSelectionModel(left.selectionModel)

        val right = resizeable(VBox())//createPerAccountHeaderView(fvm, folder, messageView)

        right.children.add(Label("RIGHT"))

        val splitPane = resizeable(SplitPane(left, right))
        splitPane.orientation = Orientation.HORIZONTAL

        //val flipper = MappedFlipper<CardDAVCard>()

        return splitPane
    }
}