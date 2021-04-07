package org.knowtiphy.pinkpigmail.util

import javafx.scene.Node
import org.controlsfx.glyphfont.FontAwesome
import org.knowtiphy.pinkpigmail.resources.Icons.style

/**
 *
 * @author graham
 */
object Mime
{
	const val TEXT = "text/*"
	const val HTML = "text/html"
	const val PLAIN = "text/plain"
	const val MULTIPART = "multipart/*"
	const val MULTIPART_ALTERNATIVE = "multipart/alternative"

	private val GLYPHS: MutableMap<String, (Int) -> Node> = HashMap()

	//	TODO -- move all the sizing into the CSS
	init
	{
		GLYPHS["application/pdf"] = { size -> style(FontAwesome.Glyph.FILE_PDF_ALT).size(size.toDouble()) }
		GLYPHS["application/vnd.oasis.opendocument.text"] = { size -> style(FontAwesome.Glyph.FILE_WORD_ALT).size(size.toDouble()) }
		GLYPHS["application/msword"] = { size -> style(FontAwesome.Glyph.FILE_WORD_ALT).size(size.toDouble()) }
		GLYPHS["application/vnd.oasis.opendocument.spreadsheet"] = { size -> style(FontAwesome.Glyph.FILE_EXCEL_ALT).size(size.toDouble()) }
		GLYPHS["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"] = { size -> style(FontAwesome.Glyph.FILE_WORD_ALT).size(size.toDouble()) }
		GLYPHS["application/rtf"] = { size -> style(FontAwesome.Glyph.FILE_TEXT_ALT).size(size.toDouble()) }
		GLYPHS["application/x-rtf"] = { size -> style(FontAwesome.Glyph.FILE_TEXT_ALT).size(size.toDouble()) }
		GLYPHS["text/plain"] = { size -> style(FontAwesome.Glyph.FILE_TEXT_ALT).size(size.toDouble()) }
		GLYPHS["text/csv"] = { size -> style(FontAwesome.Glyph.FILE_TEXT_ALT).size(size.toDouble()) }
		GLYPHS["image/jpeg"] = { size -> style(FontAwesome.Glyph.FILE_IMAGE_ALT).size(size.toDouble()) }
        GLYPHS["image/png"] = { size -> style(FontAwesome.Glyph.FILE_IMAGE_ALT).size(size.toDouble()) }
        //  TODO need a default icon
        GLYPHS["DEFAULT"] = { size -> style(FontAwesome.Glyph.SQUARE).size(size.toDouble()) }
	}

	//  TODO: there must be better way to do this safeness testing

	private val SAFE = setOf(
			"application/pdf",
			"application/rtf",
			"application/x-rtf",
			"text/plain",
			"text/csv",
			"text/turtle",
			"image/jpeg",
			"image/png",
			"application/x-turtle",
			"application/vnd.oasis.opendocument.text",
			"application/msword",
			"application/vnd.oasis.opendocument.spreadsheet",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

	private val SAFE_FILE_ENDINGS = setOf("pdf", "rtf", "txt", "csv", "jpeg", "jpg", "png", "ods", "odt", "ics", "ttl")
	private val IMAGE = setOf("image/jpeg", "image/png")

	fun glyph(mimeType: String, size: Int): Node
	{
		val g = GLYPHS[mimeType]
		return g?.invoke(size) ?: GLYPHS["DEFAULT"]!!.invoke(size)
	}

	fun isSafeFileEnding(fileName: String): Boolean
	{
		val split = fileName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		return split.size == 2 && SAFE_FILE_ENDINGS.contains(split[1])
	}

	fun isSafe(mimeType: String): Boolean
	{
		return SAFE.contains(mimeType)
	}

	fun isImage(mimeType: String): Boolean
	{
		return IMAGE.contains(mimeType)
	}
}