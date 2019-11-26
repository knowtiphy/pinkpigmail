package org.knowtiphy.pinkpigmail.resources

import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.paint.Color
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author graham
 */
object Icons
{
    const val DEFAULT_SIZE = 16
    const val MEDIUM_SIZE = 12
    const val SMALL_SIZE = 8
    // same color as modena
    val ICON_COLOR = Color.valueOf("#039ED3") as Color

    //  cache the pigs
    private val cachedPig32 = Resources::class.java.getResourceAsStream("cubed_piggy-32.png").readBytes()
    private val cachedPig128 = Resources::class.java.getResourceAsStream("cubed_piggy-128.png").readBytes()

    //  private helper functions
    private fun f(g: FontAwesome.Glyph): Glyph = Fonts.FONT.create(g)

    private fun c(g: Glyph, color: Color): Glyph = g.color(color)
    private fun s(g: Glyph, size: Int): Glyph = g.size(size.toDouble())

    fun attach(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Group
    {
        //  TODO why can't i just rotate the glyph?
        val grp = Group(s(c(f(FontAwesome.Glyph.PAPERCLIP), color), size))
        grp.rotate = 90.0
        return grp
    }

    fun reply(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.MAIL_REPLY), color), size)
    fun replyAll(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.MAIL_REPLY_ALL), color), size)
    fun forward(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.MAIL_FORWARD), color), size)
    fun delete(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.TRASH), color), size)
    fun markJunk(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.EYE), color), size)
    fun markNotJunk(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.EYE_SLASH), color), size)
    fun compose(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.PENCIL), color), size)
    fun configure(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.GEAR), color), size)
    fun switchHorizontal(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Node = s(c(f(FontAwesome.Glyph.ANGLE_DOUBLE_RIGHT), color), size)
    fun trustSender(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.THUMBS_UP), color), size)
    fun untrustSender(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.THUMBS_DOWN), color), size)
    fun loadRemote(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.DOWNLOAD), color), size)
    fun save(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.SAVE), color), size)
    fun send(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.PAPER_PLANE), color), size)
    fun trustContentProvider(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.HTML5), color), size)
    fun notification(size: Int = DEFAULT_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.EXCLAMATION_TRIANGLE), color), size)
    fun junk(size: Int = SMALL_SIZE, color: Color = Color.ORANGE): Glyph = s(c(f(FontAwesome.Glyph.CIRCLE), color), size)
    fun unread(size: Int = SMALL_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.CIRCLE), color), size)
    fun answered(size: Int = SMALL_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.REPLY), color), size)
    fun deleted(size: Int = SMALL_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.CLOSE), color), size)
    fun calendar(size: Int = SMALL_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.CALENDAR), color), size)
    fun mail(size: Int = SMALL_SIZE, color: Color = ICON_COLOR): Glyph = s(c(f(FontAwesome.Glyph.ENVELOPE), color), size)

    fun thePig128(): InputStream = ByteArrayInputStream(cachedPig128)
    fun thePig32(): InputStream = ByteArrayInputStream(cachedPig32)
}