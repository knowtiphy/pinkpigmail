package org.knowtiphy.pinkpigmail

import javafx.event.ActionEvent
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.SplitMenuButton
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.web.HTMLEditor
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.knowtiphy.pinkpigmail.model.EmailSendMode
import org.knowtiphy.pinkpigmail.model.IMessageModel
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.Operation.Companion.perform
import org.knowtiphy.pinkpigmail.util.ui.UIUtils
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.action
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.button
import java.io.File

/**
 * @author graham
 */
object ComposeMessage
{
	private val INSETS = Insets(2.0, 2.0, 2.0, 3.0)

	private fun header(model : IMessageModel) : GridPane
	{
		val subject = Label(Strings.SUBJECT)
		val cc = Label(Strings.CC)
		val to = Label(Strings.TO)

		val subjectField = TextField(model.subjectProperty().get())
		model.subjectProperty().bindBidirectional(subjectField.textProperty())
		val toField = TextField(model.toProperty().get())
		model.toProperty().bindBidirectional(toField.textProperty())
		val ccField = TextField(model.ccProperty().get())
		model.ccProperty().bindBidirectional(ccField.textProperty())

		val grid = GridPane()
		with(grid) {
			hgap = 30.0
			vgap = 3.0
			addColumn(0, to, subject, cc)
			addColumn(1, toField, subjectField, ccField)
		}

		arrayOf(subjectField, toField, ccField).forEach { GridPane.setHgrow(it, Priority.ALWAYS) }

		return grid
	}

	private fun attachFile(stage : Stage) : List<File>?
	{
		val fileChooser = FileChooser()
		fileChooser.title = Strings.ATTACH_FILE
		return fileChooser.showOpenMultipleDialog(stage)
	}

	fun compose(model : IMessageModel)
	{
		val toolBar = HBox(1.0)
		val root = VBox(5.0, header(model), toolBar)

		val scene = UIUtils.getScene(root)

		var sendAction : ((ActionEvent) -> Unit)? = null
		var saveAction : ((ActionEvent) -> Unit)? = null
		var editor : Node? = null

		val stage = UIUtils.getStage(Strings.FROM + ": " + model.account.emailAddressProperty.get(), 800.0, 700.0)
		when (model.sendMode)
		{
			EmailSendMode.TEXT ->
			{
				editor = TextArea()
				editor.text = model.contentProperty().get()
				model.contentProperty().bind(editor.textProperty())
				editor.positionCaret(1)
				sendAction = { stage.close(); model.send() }
				saveAction = { perform { model.saveToDrafts() } }
			}
			EmailSendMode.HTML ->
			{
				editor = HTMLEditor()
				editor.htmlText = model.contentProperty().get()

				sendAction = { stage.close(); model.contentProperty().set(editor.htmlText); model.send() }
				saveAction = { model.contentProperty().set(editor.htmlText); model.saveToDrafts() }
			}
		}

		VBox.setVgrow(editor, Priority.ALWAYS)
		with(root) {
			children.add(editor)
			padding = INSETS
		}

		val sendB = button(action(Icons.send(), sendAction, Strings.SEND, false))
		val attachB = SplitMenuButton()
		with(attachB) {
			setOnAction {
				attachFile(stage)?.forEach { f ->
					val attachment = OutgoingAttachment(f.toPath())
					model.attachments.add(OutgoingAttachment(f.toPath()))
					perform {
						attachB.items.add(0, Attachments.addRemoveMenu(attachment, model.attachments, attachB.items))
					}
				}
			}
			graphic = Icons.attach()
			styleClass.add(Globals.STYLE_SHEET)
		}

		perform { Attachments.addRemoveMenu(model.attachments, attachB.items) }

		val saveB = button(action(Icons.save(), saveAction, Strings.SAVE_TO_DRAFTS, false))

		toolBar.children.addAll(sendB, attachB, saveB)

		stage.scene = scene
		stage.show()
	}

	private fun createRDFModel(sModel : IMessageModel) : Model
	{
		val model = ModelFactory.createDefaultModel();

		return model
	}
}

//				//	TODO -- initialize the insert cursor at the beginning?
//				val filter = EventHandler<KeyEvent>() {
//					println("Filtering out event " + it.eventType)
//					println("ORIGINAL")
//					foo(it)
//					println(":" + it.character + ":")
//					println(":" + it.character.length + ":")
//					println(":" + (it.character.equals(KeyCode.CONTROL.toString())) + ":")
//
//					//	TODO -- why does this not detect ctrl-m????
//					if (it.character == "m" && it.isControlDown)
//					{
//						println("CCCC CONSUMING CTRL-M")
//						//xxx = it
//						//it.consume()
//					}
//
//					if (it.code == KeyCode.ENTER)
//					{
//						println("TRANSFORMING")
//						//  pretty sure this is the right key-event
////                        val ke = KeyEvent(it.source, it.target, it.eventType,
////                                xxx!!.character, xxx!!.text, xxx!!.code, xxx!!.isShiftDown, xxx!!.isControlDown,
////								xxx!!.isAltDown, xxx!!.isMetaDown);
//						val ke = KeyEvent(it.source, it.target, it.eventType,
//								"m", "m", null, false, true, false, false)
//						foo(ke)
//						later { root.scene.processKeyEvent(ke) }
//					}
//				}
//
//				root.addEventFilter(KeyEvent.KEY_TYPED, filter)
//
//private fun foo(ke: KeyEvent)
//{
//	println("START EVENT")
//	println(ke.character)
//	println(ke.code)
//	println(ke.isAltDown)
//	println(ke.isControlDown)
//	println(ke.isMetaDown)
//	println(ke.isShiftDown)
//	println(ke.isShiftDown)
//	println(ke.text)
//	println(ke.source)
//	println(ke.target)
//	println(ke.eventType)
//	println("END EVENT")
//}
