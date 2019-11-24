package org.knowtiphy.pinkpigmail.resources

import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.paint.Color
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
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

    @JvmStatic
    fun attach(size: Int): Group
    {
        //  TODO why can't i just rotate the glyph?
        val grp = Group(Fonts.FONT.create(FontAwesome.Glyph.PAPERCLIP).color(ICON_COLOR).size(size.toDouble()))
        grp.rotate = 90.0
        return grp
    }

    @JvmStatic
    fun reply(size: Int): Glyph = Fonts.FONT.create(FontAwesome.Glyph.MAIL_REPLY).color(ICON_COLOR).size(size.toDouble())

    @JvmStatic
    fun replyAll(size: Int): Glyph = Fonts.FONT.create(FontAwesome.Glyph.MAIL_REPLY_ALL).color(ICON_COLOR).size(size.toDouble())

    @JvmStatic
    fun forward(size: Int): Glyph = Fonts.FONT.create(FontAwesome.Glyph.MAIL_FORWARD).color(ICON_COLOR).size(size.toDouble())

    @JvmStatic
    fun delete(size: Int): Glyph = Fonts.FONT.create(FontAwesome.Glyph.TRASH).color(ICON_COLOR).size(size.toDouble())

    @JvmStatic
    fun markJunk(size: Int): Glyph
    {
        return Fonts.FONT.create(FontAwesome.Glyph.EYE).color(ICON_COLOR).size(size.toDouble())
    }

    @JvmStatic
    fun markNotJunk(size: Int): Glyph
    {
        return Fonts.FONT.create(FontAwesome.Glyph.EYE_SLASH).color(ICON_COLOR).size(size.toDouble())
    }

    @JvmStatic
    fun compose(size: Int): Glyph = Fonts.FONT.create(FontAwesome.Glyph.PENCIL).color(ICON_COLOR).size(size.toDouble())

    @JvmStatic
    fun configure(size: Int): Glyph = Fonts.FONT.create(FontAwesome.Glyph.GEAR).color(ICON_COLOR).size(size.toDouble())

    @JvmStatic
    fun switchHorizontal(size: Int): Node
    {
        return Fonts.FONT.create(FontAwesome.Glyph.ANGLE_DOUBLE_RIGHT).color(ICON_COLOR).size(size.toDouble())
    }

    @JvmStatic
    fun trustSender(size: Int): Glyph
    {
        return Fonts.FONT.create(FontAwesome.Glyph.THUMBS_UP).color(ICON_COLOR).size(size.toDouble())
    }

    @JvmStatic
    fun untrustSender(size: Int): Glyph
    {
        return Fonts.FONT.create(FontAwesome.Glyph.THUMBS_DOWN).color(ICON_COLOR).size(size.toDouble())
    }

    @JvmStatic
    fun loadRemote(size: Int): Glyph
    {
        return Fonts.FONT.create(FontAwesome.Glyph.DOWNLOAD).color(ICON_COLOR).size(size.toDouble())
    }

    @JvmStatic
    fun save(size: Int): Glyph
    {
        return Fonts.FONT.create(FontAwesome.Glyph.SAVE).color(ICON_COLOR).size(size.toDouble())
    }

    @JvmStatic
    fun send(size: Int): Glyph = Fonts.FONT.create(FontAwesome.Glyph.PAPER_PLANE).color(ICON_COLOR).size(size.toDouble())

    @JvmStatic
    fun trustContentProvider(size: Int): Glyph = Fonts.FONT.create(FontAwesome.Glyph.HTML5).color(ICON_COLOR).size(size.toDouble())

    @JvmStatic
    fun notification(size: Int): Glyph
    {
        return Fonts.FONT.create(FontAwesome.Glyph.EXCLAMATION_TRIANGLE).color(ICON_COLOR).size(size.toDouble())
    }

    @JvmStatic
    fun junk(size: Int = SMALL_SIZE): Glyph = Fonts.FONT.create(FontAwesome.Glyph.CIRCLE).color(Color.ORANGE).size(size.toDouble())

    @JvmStatic
    fun unread(size: Int = SMALL_SIZE): Glyph = Fonts.FONT.create(FontAwesome.Glyph.CIRCLE).color(ICON_COLOR).size(size.toDouble())

    @JvmStatic
    fun answered(size: Int = SMALL_SIZE): Glyph = Fonts.FONT.create(FontAwesome.Glyph.REPLY).color(ICON_COLOR).size(size.toDouble())

    @JvmStatic
    fun deleted(size: Int = SMALL_SIZE): Glyph = Fonts.FONT.create(FontAwesome.Glyph.CLOSE).color(ICON_COLOR).size(size.toDouble())

    @JvmStatic
    fun calendar(size: Int = SMALL_SIZE): Glyph = Fonts.FONT.create(FontAwesome.Glyph.CALENDAR).color(ICON_COLOR).size(size.toDouble())

    @JvmStatic
    fun mail(size: Int = SMALL_SIZE): Glyph = Fonts.FONT.create(FontAwesome.Glyph.ENVELOPE).color(ICON_COLOR).size(size.toDouble())

    @JvmStatic
    fun thePig128(): InputStream
    {
        return Resources::class.java.getResourceAsStream("cubed_piggy-128.png")
    }

    @JvmStatic
    fun thePig32(): InputStream
    {
        return Resources::class.java.getResourceAsStream("cubed_piggy-32.png")
    }
}