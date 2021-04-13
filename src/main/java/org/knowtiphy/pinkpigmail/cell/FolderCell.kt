package org.knowtiphy.pinkpigmail.cell

import com.sun.javafx.embed.swing.oldimpl.InteropFactoryO
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
import org.knowtiphy.pinkpigmail.model.events.FinishSyncEvent
import org.knowtiphy.pinkpigmail.model.events.StartSyncEvent
import org.knowtiphy.pinkpigmail.resources.Fonts.DEFAULT_FONT
import org.knowtiphy.pinkpigmail.resources.Icons

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
	private val sync = Icons.refresh()
	private val folderGrid = GridPane()
	private val spacer = HBox()
	private val folderBox = HBox(nameLabel, spacer, folderGrid)

	init
	{
		account.style = "-fx-font-weight: bold"
		accountGrid.alignment = Pos.CENTER_LEFT

		sync.visibleProperty().set(false)
		folderGrid.addRow(0, sync, unreadLabel, Label("/"), countLabel)
		folderGrid.hgap = 5.0
		//GridPane.setVgrow(sync, Priority.NEVER)
		HBox.setHgrow(spacer, Priority.ALWAYS)
		folderBox.spacing = 5.0
		folderBox.padding = Insets.EMPTY
		folderBox.alignment = Pos.CENTER_LEFT
		countLabel.textFill = Color.GREY
		countLabel.font = Font.font(DEFAULT_FONT.family, 11.0)
		unreadLabel.textFill = Color.RED
		unreadLabel.font = Font.font(DEFAULT_FONT.family, 11.0)
	}

	private fun removePreviousSetup()
	{
		countLabel.textProperty().unbind()
		unreadLabel.textProperty().unbind()
		accountGrid.children.clear()
	}

	override fun updateItem(item : IFolder?, empty : Boolean)
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
			// TODO: this needs to be better - its butt ugly at the moment -- esp unsubsribing
			item.account.events.filter(StartSyncEvent::class.java).filter { it.synced == item }
				.subscribe { sync.visibleProperty().set(true) }

			item.account.events.filter(FinishSyncEvent::class.java).filter { it.synced == item }
				.subscribe { sync.visibleProperty().set(false) }

			nameLabel.textProperty().bind(item.nameProperty)
			countLabel.textProperty().bind(item.messageCountProperty.asString())
			unreadLabel.textProperty().bind(item.unreadMessageCountProperty.asString())

			graphic = folderBox
		}
	}
}
