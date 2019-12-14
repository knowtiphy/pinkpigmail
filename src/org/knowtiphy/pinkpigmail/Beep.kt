package org.knowtiphy.pinkpigmail

import javafx.scene.media.AudioClip
import org.knowtiphy.pinkpigmail.resources.Resources
import org.knowtiphy.pinkpigmail.util.Operation

object Beep
{
    private val BEEP: AudioClip? = AudioClip(Resources::class.java.getResource("beep-29.wav").toString())

    fun beep() = Operation.perform { BEEP?.play() }
}