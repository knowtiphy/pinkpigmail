package org.knowtiphy.pinkpigmail.util

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.paint.Paint
import javafx.stage.Stage
import org.knowtiphy.pinkpigmail.resources.Icons
import java.util.concurrent.Callable

/**
 *
 * @author graham
 */
object UIUtils
{
    @JvmStatic
    fun spacer(): Node
    {
        val box = HBox()
        box.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
        return box
    }

    @JvmStatic
    fun resizable(node: Region)
    {
        node.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
        node.setMinSize(0.0, 0.0)
    }

    fun <T> callable(f: () -> T): Callable<T>
    {
        return Callable { f.invoke() }
    }

    fun <T> comparator(f: (T, T) -> Int): Comparator<T>
    {
        return Comparator<T> { a: T, b: T -> f.invoke(a, b) }
    }

    @JvmStatic
    fun stage(): Stage
    {
        val stage = Stage()
        stage.icons.add(Image(Icons.thePig128()))
        return stage
    }

    @JvmStatic
    fun getStage(width: Double, height: Double): Stage
    {
        val stage = stage()
        stage.minWidth = width
        stage.minHeight = height
        return stage
    }

    @JvmStatic
    fun labelInBox(text: String, color: Paint): Node
    {
        val label = Label(text)
        label.alignment = Pos.CENTER
        label.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
        //        HBox box = new HBox(label);
        //        HBox.setHgrow(label, Priority.ALWAYS);
        //        box.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        label.background = Background(BackgroundFill(color, null, null))
        return label
    }
}