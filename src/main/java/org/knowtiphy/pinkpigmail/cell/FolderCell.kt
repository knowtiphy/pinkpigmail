package org.knowtiphy.pinkpigmail.cell

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.TreeCell
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import org.knowtiphy.pinkpigmail.model.IFolder

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
    private val folderGrid = GridPane()
    private val spacer = HBox()
    private val folderBox = HBox(nameLabel, spacer, folderGrid)

    init
    {
        account.style = "-fx-font-weight: bold"
        accountGrid.alignment = Pos.CENTER_LEFT

        folderGrid.addRow(0, unreadLabel, Label("/"), countLabel)
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
            nameLabel.textProperty().bind(folder!!.nameProperty)
            countLabel.textProperty().bind(folder.messageCountProperty.asString())
            unreadLabel.textProperty().bind(folder.unreadMessageCountProperty.asString())
            graphic = folderBox
        }
    }
}
