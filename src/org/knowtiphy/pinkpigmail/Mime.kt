package org.knowtiphy.pinkpigmail

import javafx.scene.Node
import org.controlsfx.glyphfont.FontAwesome
import org.knowtiphy.pinkpigmail.resources.Fonts
import org.knowtiphy.pinkpigmail.resources.Icons
import java.util.*

/**
 *
 * @author graham
 */
object Mime
{
    private val GLYPHS: MutableMap<String, (Int) -> Node> = HashMap()

    init
    {
        GLYPHS["application/pdf"] = { size -> Fonts.FONT.create(FontAwesome.Glyph.FILE_PDF_ALT).color(Icons.ICON_COLOR).size(size.toDouble()) }
        GLYPHS["application/vnd.oasis.opendocument.text"] = { size -> Fonts.FONT.create(FontAwesome.Glyph.FILE_WORD_ALT).color(Icons.ICON_COLOR).size(size.toDouble()) }
        GLYPHS["application/msword"] = { size -> Fonts.FONT.create(FontAwesome.Glyph.FILE_WORD_ALT).color(Icons.ICON_COLOR).size(size.toDouble()) }
        GLYPHS["application/vnd.oasis.opendocument.spreadsheet"] = { size -> Fonts.FONT.create(FontAwesome.Glyph.FILE_EXCEL_ALT).color(Icons.ICON_COLOR).size(size.toDouble()) }
        GLYPHS["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"] = { size -> Fonts.FONT.create(FontAwesome.Glyph.FILE_WORD_ALT).color(Icons.ICON_COLOR).size(size.toDouble()) }
        GLYPHS["application/rtf"] = { size -> Fonts.FONT.create(FontAwesome.Glyph.FILE_TEXT_ALT).color(Icons.ICON_COLOR).size(size.toDouble()) }
        GLYPHS["application/x-rtf"] = { size -> Fonts.FONT.create(FontAwesome.Glyph.FILE_TEXT_ALT).color(Icons.ICON_COLOR).size(size.toDouble()) }
        GLYPHS["text/plain"] = { size -> Fonts.FONT.create(FontAwesome.Glyph.FILE_TEXT_ALT).color(Icons.ICON_COLOR).size(size.toDouble()) }
        GLYPHS["text/csv"] = { size -> Fonts.FONT.create(FontAwesome.Glyph.FILE_TEXT_ALT).color(Icons.ICON_COLOR).size(size.toDouble()) }
        GLYPHS["image/jpeg"] = { size -> Fonts.FONT.create(FontAwesome.Glyph.FILE_IMAGE_ALT).color(Icons.ICON_COLOR).size(size.toDouble()) }
        GLYPHS["image/png"] = { size -> Fonts.FONT.create(FontAwesome.Glyph.FILE_IMAGE_ALT).color(Icons.ICON_COLOR).size(size.toDouble()) }
    }

    //  TODO: there must be better way to do this safeness testing

    private val SAFE = setOf(
            "application/pdf",
            "application/rtf",
            "application/x-rtf",
            "text/plain",
            "text/csv",
            "image/jpeg",
            "image/png",
            "application/vnd.oasis.opendocument.text",
            "application/msword",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

    private val SAFE_FILE_ENDINGS = setOf("pdf", "rtf", "txt", "csv", "jpeg", "jpg", "png", "ods", "odt", "ics")
    private val IMAGE = setOf("image/jpeg", "image/png")

    @JvmStatic
    fun glyph(mimeType: String, size: Int): Node
    {
        val g = GLYPHS[mimeType]
        return g?.invoke(size) ?: Icons.attach(size)
    }

    @JvmStatic
    fun isSafeFileEnding(fileName: String): Boolean
    {
        val split = fileName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return split.size == 2 && SAFE_FILE_ENDINGS.contains(split[1])
    }

    @JvmStatic
    fun isSafe(mimeType: String): Boolean
    {
        return SAFE.contains(mimeType)
    }

    @JvmStatic
    fun isImage(mimeType: String): Boolean
    {
        return IMAGE.contains(mimeType)
    }
}