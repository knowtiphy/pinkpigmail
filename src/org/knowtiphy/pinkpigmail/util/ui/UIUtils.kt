package org.knowtiphy.pinkpigmail.util.ui

import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.stage.Stage
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.resources.Icons
import java.util.*
import java.util.concurrent.Callable

/**
 *
 * @author graham
 */
object UIUtils
{
    fun hSpacer(): Node
    {
        val box = maxSizeable(HBox())
        HBox.setHgrow(box, Priority.ALWAYS)
        return box
    }

    fun vSpacer(): Node
    {
        val box = maxSizeable(VBox())
        VBox.setVgrow(box, Priority.ALWAYS)
        return box
    }

    fun <T : Region> resizeable(node: T): T
    {
        node.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
        node.setMinSize(0.0, 0.0)
        return node
    }

    fun <T : Region> maxSizeable(node: T): T
    {
        node.setMaxSize(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE)
        return node
    }

    fun <T> callable(f: () -> T): Callable<T>
    {
        return Callable { f.invoke() }
    }

    fun <T : Comparable<T>> cmp(e: (IMessage) -> T?): Comparator<IMessage>
    {
        return kotlin.Comparator { x, y ->
            val a = e.invoke(x)
            val b = e.invoke(y)
            val result = if (a == null) if (b == null) 0 else 1 else if (b == null) -1 else a.compareTo(b)
//            println((a?.toString() ?: "NULL") + " : " + (b?.toString() ?: "NULL") + " : " + result)
            result
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

    fun getStage(title : String, width: Double, height: Double): Stage
    {
        val stage = getStage(width, height)
        stage.title = title
        return stage
    }
    fun later(x: () -> Unit) = Platform.runLater(x)

    fun boxIt(node: Node, color: Paint = Color.WHITE): GridPane
    {
        val b = resizeable(BorderPane())
        b.background = Background(BackgroundFill(color, null, null))
        b.center = node

        val g = GridPane()
        with(g) {
            addRow(0, b)
            alignment = Pos.CENTER
        }

        GridPane.setHgrow(b, Priority.ALWAYS)
        GridPane.setVgrow(b, Priority.ALWAYS)

        return resizeable(g)
    }
}