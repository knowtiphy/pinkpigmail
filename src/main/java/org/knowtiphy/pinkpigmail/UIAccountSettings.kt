package org.knowtiphy.pinkpigmail

import javafx.beans.property.SimpleDoubleProperty
import org.apache.jena.rdf.model.Model
import org.knowtiphy.utils.JenaUtils

class UIAccountSettings
{
	companion object
	{
		private const val DEFAULT_VERTICAL_DIVIDER_POSITION = 0.15
	}

	val verticalPos = SimpleDoubleProperty(DEFAULT_VERTICAL_DIVIDER_POSITION)

	fun save(model: Model, uaid: String)
	{
		JenaUtils.addDP(model, uaid, UIVocabulary.HAS_VERTICAL_POSITION, verticalPos.get())
	}

	fun read(model: Model, uaid: String)
	{
		verticalPos.set(JenaUtils.getOD(model, uaid, UIVocabulary.HAS_VERTICAL_POSITION, DEFAULT_VERTICAL_DIVIDER_POSITION))
	}
}