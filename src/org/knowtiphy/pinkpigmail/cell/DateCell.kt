package org.knowtiphy.pinkpigmail.cell

import javafx.beans.property.ObjectProperty
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.util.Format

import java.time.LocalDate

/**
 * @author graham
 */
class DateCell(extractor: (IMessage) -> ObjectProperty<LocalDate>) :
        GeneralCell<LocalDate, ObjectProperty<LocalDate>>(extractor,
                { date: ObjectProperty<LocalDate> -> Format.format(date.get()) })
