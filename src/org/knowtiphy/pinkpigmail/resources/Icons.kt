package org.knowtiphy.pinkpigmail.resources

import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author graham
 */
object Icons
{
	const val STD_ICON_STYLE_CLASS = "stdIcon"
	const val JUNK_ICON_STYLE_CLASS = "junkIcon"
	const val SMALL_ICON_STYLE_CLASS = "smallIcon"

	const val SMALL_SIZE = 8

	//  cache the pigs
	private val cachedPig32 = Resources::class.java.getResourceAsStream("cubed_piggy-32.png").readBytes()
	private val cachedPig128 = Resources::class.java.getResourceAsStream("cubed_piggy-128.png").readBytes()

	fun style(glyph: FontAwesome.Glyph, styleClass: String = STD_ICON_STYLE_CLASS): Glyph
	{
		val result = Fonts.FONT.create(glyph)
//		result.styleClass.remove("label");
		result.styleClass.add(styleClass)
		return result
	}

	fun attach(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.PAPERCLIP, styleClass)
	fun reply(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.MAIL_REPLY, styleClass)
	fun replyAll(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.MAIL_REPLY_ALL, styleClass)
	fun forward(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.MAIL_FORWARD, styleClass)
	fun delete(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.TRASH, styleClass)
	fun markJunk(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.EYE, styleClass)
	fun markNotJunk(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.EYE_SLASH, styleClass)
	fun compose(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.PENCIL, styleClass)
	fun trustSender(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.THUMBS_UP, styleClass)
	fun loadRemote(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.DOWNLOAD, styleClass)
	fun save(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.SAVE, styleClass)
	fun send(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.PAPER_PLANE, styleClass)
	fun trustContentProvider(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.HTML5, styleClass)
	fun calendar(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.CALENDAR, styleClass)
	fun book(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.BOOK, styleClass)
	fun mail(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.ENVELOPE, styleClass)
	fun switchHorizontal(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.ANGLE_DOUBLE_RIGHT, styleClass)
	fun configure(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.GEAR, styleClass)
	fun notification(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.EXCLAMATION_TRIANGLE, styleClass)
	fun untrustSender(styleClass: String = STD_ICON_STYLE_CLASS) = style(FontAwesome.Glyph.THUMBS_DOWN, styleClass)
	//	TODO -- move the sizing into the CSS
	fun junk(styleClass: String = JUNK_ICON_STYLE_CLASS): Glyph = style(FontAwesome.Glyph.CIRCLE, styleClass).size(8.0)
	fun unread(styleClass: String = SMALL_ICON_STYLE_CLASS): Glyph = style(FontAwesome.Glyph.CIRCLE, styleClass).size(8.0)
	fun answered(styleClass: String = SMALL_ICON_STYLE_CLASS): Glyph = style(FontAwesome.Glyph.REPLY, styleClass).size(8.0)
	fun deleted(styleClass: String = SMALL_ICON_STYLE_CLASS): Glyph = style(FontAwesome.Glyph.CLOSE, styleClass).size(8.0)

	fun thePig128(): InputStream = ByteArrayInputStream(cachedPig128)
	fun thePig32(): InputStream = ByteArrayInputStream(cachedPig32)
}