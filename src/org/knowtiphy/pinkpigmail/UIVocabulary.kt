package org.knowtiphy.pinkpigmail

/**
 *
 * @author graham
 */
object UIVocabulary
{
    const val RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"

    private const val BASE = "http://www.knowtiphy.org/"
    private const val TBASE = BASE + "Terminology/"

    const val UI_SETTING = TBASE + "UISetting"
    const val FOLDER_PREFERENCE = TBASE + "FolderPreference"

    const val HAS_WIDTH = TBASE + "hasWidth"
    const val HAS_HEIGHT = TBASE + "hasHeight"
    const val HAS_VERTICAL_POSITION = TBASE + "hasVerticalPosition"

    const val HAS_FOLDER_PREFERENCE = TBASE + "hasFolderPreference"
    const val FOR = TBASE + "for"
    const val HAS_VIEW_TYPE = TBASE + "hasViewType"
    const val HAS_HORIZONTAL_POSITION = TBASE + "hasHorizontalPosition"
}
