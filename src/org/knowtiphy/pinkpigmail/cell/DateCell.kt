package org.knowtiphy.pinkpigmail.cell

import javafx.beans.property.ObjectProperty
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.util.Format
import java.time.ZonedDateTime

/**
 * @author graham
 */
class DateCell(extractor: (IMessage) -> ObjectProperty<ZonedDateTime>) :
        GeneralCell<ZonedDateTime, ObjectProperty<ZonedDateTime>>(extractor,
                { date: ObjectProperty<ZonedDateTime> -> Format.format(date.get()) })
