package org.knowtiphy.pinkpigmail.calendarview

import com.dlsc.formsfx.view.controls.SimpleControl
import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.scene.layout.StackPane
import tornadofx.control.DateTimePicker

/**
 * This class provides a specific implementation to edit date values.
 *
 * @author Tomasz Krzemiński
 */
class SimpleDateTimeControl : SimpleControl<DateTimeField>()
{
	private var stack : StackPane? = null
	private var picker : DateTimePicker? = null
	private var readOnlyLabel : Label? = null
	private var fieldLabel : Label? = null

	override fun initializeParts()
	{
		stack = StackPane()

		fieldLabel = Label()
		readOnlyLabel = Label()
		picker = DateTimePicker()
		picker!!.isEditable = true
	}

	override fun layoutParts()
	{
		super.layoutParts()

		val columns = field.span
		readOnlyLabel!!.styleClass.add("read-only-label")

		picker!!.maxWidth = Double.MAX_VALUE

		stack!!.alignment = Pos.CENTER_LEFT
		stack!!.children.addAll(picker, readOnlyLabel)

		val labelDescription = field.labelDescription
		val valueDescription = field.valueDescription

		add(fieldLabel, 0, 0, 2, 1)
		if (labelDescription != null)
		{
			GridPane.setValignment(labelDescription, VPos.TOP)
			add(labelDescription, 0, 1, 2, 1)
		}
		add(stack, 2, 0, columns - 2, 1)
		if (valueDescription != null)
		{
			GridPane.setValignment(valueDescription, VPos.TOP)
			add(valueDescription, 2, 1, columns - 2, 1)
		}
	}

	override fun setupBindings()
	{
		super.setupBindings()
		picker!!.disableProperty().bind(field.editableProperty().not())
		readOnlyLabel!!.visibleProperty().bind(field.editableProperty().not())

		picker!!.editor.textProperty().bindBidirectional(field.userInputProperty())
		fieldLabel!!.textProperty().bind(field.labelProperty())
		picker!!.promptTextProperty().bind(field.placeholderProperty())
		picker!!.managedProperty().bind(picker!!.visibleProperty())
	}

	/**
	 * {@inheritDoc}
	 */
	override fun setupEventHandlers()
	{
		picker!!.editor.textProperty()
			.addListener { _ : ObservableValue<out String>?, _ : String?, newValue : String ->
				field!!.userInputProperty().value = newValue
			}
	}
}