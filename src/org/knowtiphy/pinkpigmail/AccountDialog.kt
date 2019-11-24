package org.knowtiphy.pinkpigmail

import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import org.controlsfx.tools.Borders
import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.utils.Pair

/**
 *
 * @author graham
 */
object AccountDialog
{
    fun create(accounts: Collection<IAccount>): Pair<Map<IAccount, StringProperty>, Group>
    {
        val box = VBox()
        box.spacing = 20.0

        val properties = HashMap<IAccount, StringProperty>()
        var i = 0
        for (account in accounts)
        {
            val row = GridPane()
            row.hgap = 20.0
            row.alignment = Pos.CENTER_LEFT

            val progressSpinner = ProgressIndicator()
            progressSpinner.setMaxSize(150.0, 150.0)

            val serverName = Text("FOO")//account.emailAddressProperty.get())
            serverName.style = "-fx-font-weight: bold"

            val message = SimpleStringProperty()
            properties[account] = message
            //  TODO -- size this correctly
            val label = Label()
            label.textProperty().bindBidirectional(message)
          //  label.setPrefSize(300.0, 150.0)
            label.alignment = Pos.CENTER_LEFT

            row.addRow(i++, progressSpinner, serverName, label)
            box.children.add(row)
        }

        box.alignment = Pos.CENTER
        box.padding = Insets(20.0, 20.0, 20.0, 20.0)

        return Pair(properties, Group(Borders.wrap(box).lineBorder().color(Color.BLACK).buildAll()))
    }
}
