package org.knowtiphy.pinkpigmail

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.SplitPane
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.knowtiphy.utils.JenaUtils

class FolderSettings(val viewType: String = DEFAULT_VIEW,
                     val horizontalPositions: ObservableList<SplitPane.Divider> = makeDivider(DEFAULT_HORIZONTAL_POSITION),
                     private val verticalPositions: ObservableList<SplitPane.Divider> = makeDivider(DEFAULT_VERTICAL_POSITION))
{
    fun save(model: Model, pref: Resource)
    {
        model.add(pref, model.createProperty(UIVocabulary.HAS_VIEW_TYPE), model.createTypedLiteral(viewType))
        model.add(pref, model.createProperty(UIVocabulary.HAS_HORIZONTAL_POSITION),
                model.createTypedLiteral(horizontalPositions[0].position))
        model.add(pref, model.createProperty(UIVocabulary.HAS_VERTICAL_POSITION),
                model.createTypedLiteral(verticalPositions[0].position))
    }

    companion object
    {
        const val VERTICAL_VIEW = "0"
        const val HORIZONTAL_VIEW = "1"

        const val DEFAULT_VIEW = HORIZONTAL_VIEW
        const val DEFAULT_HORIZONTAL_POSITION = 0.5
        const val DEFAULT_VERTICAL_POSITION = 0.15

        private fun makeDivider(value: Double): ObservableList<SplitPane.Divider>
        {
            val div = SplitPane.Divider()
            div.position = value
            return FXCollections.observableArrayList(div)
        }

        fun read(model: Model, pref: Resource): FolderSettings
        {
            val viewType = DEFAULT_VIEW//JenaUtils.getI(JenaUtils.listObjectsOfPropertyU(model, pref.toString(), UIVocabulary.HAS_VIEW_TYPE));
            val hPos = makeDivider(JenaUtils.getD(model, pref.toString(), UIVocabulary.HAS_HORIZONTAL_POSITION, DEFAULT_HORIZONTAL_POSITION))
            val vPos = makeDivider(JenaUtils.getD(model, pref.toString(), UIVocabulary.HAS_VERTICAL_POSITION, DEFAULT_VERTICAL_POSITION))
            return FolderSettings(viewType, hPos, vPos)
        }
    }
}