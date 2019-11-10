package org.knowtiphy.pinkpigmail.resources

import org.controlsfx.glyphfont.GlyphFont
import org.controlsfx.glyphfont.GlyphFontRegistry

/**
 *
 * @author graham
 */
interface Fonts
{
    companion object
    {
        private val FONT_AWESOME = GlyphFontRegistry.font("FontAwesome") as GlyphFont

        val FONT = FONT_AWESOME
    }
}