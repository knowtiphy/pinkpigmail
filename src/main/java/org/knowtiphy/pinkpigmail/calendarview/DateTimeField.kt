package org.knowtiphy.pinkpigmail.calendarview

import com.dlsc.formsfx.model.structure.DataField
import javafx.beans.property.ObjectProperty
import javafx.util.converter.LocalDateTimeStringConverter
import java.time.LocalDateTime
import java.time.format.FormatStyle

/**
 * This class provides an implementation of a [Field] containing a `LocalDateTime` value.
 */
class DateTimeField(valueProperty : ObjectProperty<LocalDateTime?>?, persistentValueProperty : ObjectProperty<LocalDateTime?>?) :
	DataField<ObjectProperty<LocalDateTime?>?, LocalDateTime?, DateTimeField>(valueProperty, persistentValueProperty)
{
	init
	{
		stringConverter = LocalDateTimeStringConverter(FormatStyle.SHORT, FormatStyle.SHORT)
		renderer = SimpleDateTimeControl()
		userInput.value = null
		userInput.value = stringConverter.toString(persistentValue!!.value)
	}
}