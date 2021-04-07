package org.knowtiphy.pinkpigmail.cell

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TreeCell
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.pinkpigmail.model.events.FolderSyncDoneEvent
import org.knowtiphy.pinkpigmail.model.events.FolderSyncStartedEvent
import org.knowtiphy.pinkpigmail.util.ui.WaitSpinner
import tornadofx.progressbar

/**
 * @author graham
 */
class FolderCell : TreeCell<IFolder>()
{
    private val account = Text()
    private val accountGrid = GridPane()
    private val countLabel = Label()
    private val unreadLabel = Label()
    private val nameLabel = Label()
    private val sync = WaitSpinner("", ProgressBar(0.0))
    private val folderGrid = GridPane()
    private val spacer = HBox()
    private val folderBox = HBox(nameLabel, spacer, folderGrid)

    init
    {
        account.style = "-fx-font-weight: bold"
        accountGrid.alignment = Pos.CENTER_LEFT

        folderGrid.addRow(0, unreadLabel, Label("/"), countLabel, sync)
        HBox.setHgrow(spacer, Priority.ALWAYS)
        folderBox.spacing = 5.0
        folderBox.padding = Insets.EMPTY
        folderBox.alignment = Pos.CENTER_LEFT
        countLabel.textFill = Color.GREY
        val stdFont = unreadLabel.font
        countLabel.font = Font.font(stdFont.family, 11.0)
        unreadLabel.textFill = Color.RED
        unreadLabel.font = Font.font(stdFont.family, 11.0)
    }

    private fun removePreviousSetup()
    {
        countLabel.textProperty().unbind()
        unreadLabel.textProperty().unbind()
        accountGrid.children.clear()
    }

    override fun updateItem(item: IFolder?, empty: Boolean)
    {
        super.updateItem(item, empty)
        removePreviousSetup()

        //	invisible root is null, and probably some JavaFX internals
        text = null
        if (empty || item == null)
        {
            graphic = null
        } else
        {
            // TODO: this needs to be better - its butt ugly at the moment
            val folder = item as IFolder?
            folder!!.account.events.filter(FolderSyncStartedEvent::class.java).filter {
                it.folder == folder}.
            subscribe {
                println(folder.toString() + " START ");  sync.indicator.progress = 0.0 }

            folder!!.account.events.filter(FolderSyncStartedEvent::class.java).filter {
                it.folder == folder}.
            subscribe {
                println(folder.toString() + " DONE ");  sync.indicator.progress = 1.0 }

            nameLabel.textProperty().bind(folder!!.nameProperty)
            countLabel.textProperty().bind(folder.messageCountProperty.asString())
            unreadLabel.textProperty().bind(folder.unreadMessageCountProperty.asString())
            graphic = folderBox
        }
    }
}
