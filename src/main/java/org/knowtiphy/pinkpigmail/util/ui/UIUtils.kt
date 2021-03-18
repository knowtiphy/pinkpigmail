package org.knowtiphy.pinkpigmail.util.ui

import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.stage.Stage
import org.controlsfx.control.action.Action
import org.controlsfx.control.action.ActionUtils
import org.knowtiphy.pinkpigmail.PinkPigMail
import org.knowtiphy.pinkpigmail.StyleSheets
import org.knowtiphy.pinkpigmail.resources.Icons

/**
 *
 * @author graham
 */
object UIUtils
{
	fun hSpacer(): Node
	{
		val box = maxSizeable(HBox())
		HBox.setHgrow(box, Priority.ALWAYS)
		return box
	}

	fun vSpacer(): Node
	{
		val box = maxSizeable(VBox())
		VBox.setVgrow(box, Priority.ALWAYS)
		return box
	}

	fun <T : Region> resizeable(node: T): T
	{
		node.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
		node.setMinSize(0.0, 0.0)
		return node
	}

	fun <T : Region> maxSizeable(node: T): T
	{
		node.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
		return node
	}

	fun stage(): Stage
	{
		val stage = Stage()
		stage.icons.add(Image(Icons.thePig128()))
		return stage
	}

	fun getStage(width: Double, height: Double): Stage
	{
		val stage = stage()
		stage.minWidth = width
		stage.minHeight = height
		return stage
	}

	fun getStage(title: String, width: Double, height: Double): Stage
	{
		val stage = getStage(width, height)
		stage.title = title
		return stage
	}

	fun getScene(root: Parent): Scene
	{
		val scene = Scene(root)
		scene.stylesheets.add(UIUtils::class.java.getResource(PinkPigMail.STYLE_SHEET).toExternalForm())
		return scene
	}

	fun later(x: () -> Unit) = Platform.runLater(x)

	fun boxIt(node: Node, color: Paint = Color.WHITE): GridPane
	{
		val b = resizeable(BorderPane())
		b.background = Background(BackgroundFill(color, null, null))
		b.center = node

		val g = GridPane()
		with(g) {
			addRow(0, b)
			alignment = Pos.CENTER
		}

		GridPane.setHgrow(b, Priority.ALWAYS)
		GridPane.setVgrow(b, Priority.ALWAYS)

		return resizeable(g)
	}

	//	create a button from an action and a style class

	fun button(action: Action, styleClass: String = StyleSheets.STANDARD_BUTTON_STYLE_CLASS): Button
	{
		val b = ActionUtils.createButton(action)
		b.styleClass.add(styleClass)
		return b
	}

	//	functions to create actions

	private fun action(node: Node, handler: (ActionEvent) -> Unit, text: String?, tip: String, disabled: Boolean): Action
	{
		val action = Action(text) { handler.invoke(it) }
		with(action) {
			graphic = node
			longText = tip
			isDisabled = disabled
		}

		return action
	}

	fun action(node: Node, handler: (ActionEvent) -> Unit, tip: String, disabled: Boolean = true) = action(node, handler, null, tip, disabled)

	fun action(node: Node, handler: (ActionEvent) -> Unit, tip: String) = action(node, handler, null, tip, false)
}