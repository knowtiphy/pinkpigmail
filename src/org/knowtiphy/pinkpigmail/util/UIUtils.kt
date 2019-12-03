package org.knowtiphy.pinkpigmail.util

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.*
import javafx.scene.paint.Color
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
    fun spacer(): Node
    {
        val box = HBox()
        box.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
        HBox.setHgrow(box, Priority.ALWAYS)
        return box
    }

    fun resizable(node: Region)
    {
        node.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
        node.setMinSize(0.0, 0.0)
    }
//
//    fun resizable(node: Node)
//    {
//        node.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
//        node.setMinSize(0.0, 0.0)
//    }

    fun <T> callable(f: () -> T): Callable<T>
    {
        return Callable { f.invoke() }
    }

    fun <T> comparator(f: (T, T) -> Int): Comparator<T>
    {
        return Comparator<T> { a: T, b: T ->
            try
            {
                f.invoke(a, b)
            } catch (ex: Exception)
            {
                ex.printStackTrace()
                0
            }
        }
    }

    fun stage(): Stage
    {
        val stage = Stage()
        stage.icons.add(Image(Icons.thePig128()))
        return stage
    }

    fun getStage(width: Double, height: Double): Stage
    {
        val stage = stage()
        stage.minWidth = width
        stage.minHeight = height
        return stage
    }

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

    fun boxIt(node: Node, color: Paint = Color.WHITE): GridPane
    {
        val g = GridPane()
        val b = BorderPane()
        b.background = Background(BackgroundFill(color, null, null))
        b.center = node
        GridPane.setHgrow(b, Priority.ALWAYS);
        GridPane.setVgrow(b, Priority.ALWAYS);
        with(g)
        {
            addRow(0, b)
            alignment = Pos.CENTER
        }
        resizable(b)
        resizable(g)
        return g;

//        val label = Label(text)
//        label.alignment = Pos.CENTER
//        label.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
//        //        HBox box = new HBox(label);
//        //        HBox.setHgrow(label, Priority.ALWAYS);
//        //        box.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
//        label.background = Background(BackgroundFill(color, null, null))
//        return label
    }
}