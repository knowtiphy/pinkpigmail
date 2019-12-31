package org.knowtiphy.pinkpigmail

import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.collections.FXCollections
import javafx.scene.control.SplitPane.Divider
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.knowtiphy.babbage.Babbage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.utils.JenaUtils
import org.knowtiphy.utils.NameSource
import org.knowtiphy.utils.OS
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * @author graham
 */
class UISettings
{
    val verticalPosition: MutableList<Divider> = FXCollections.observableArrayList()
    //  have to set defaults here in case we boot with no UI settings at all
    val widthProperty: DoubleProperty = SimpleDoubleProperty(DEFAULT_WIDTH.toDouble())
    val heightProperty: DoubleProperty = SimpleDoubleProperty(DEFAULT_HEIGHT.toDouble())

    private val folderSettings = HashMap<String, FolderSettings>()

    private fun saveFolderSettings(model: Model, ui: Resource, names: NameSource, folder: IFolder)
    {
        val fSettings = folderSettings[folder.id]
        if (fSettings != null)
        {
            val pref = model.createResource(names.get())
            model.add(ui, model.createProperty(UIVocabulary.HAS_FOLDER_PREFERENCE), pref)
            model.add(pref, model.createProperty(UIVocabulary.RDF_TYPE), model.createResource(UIVocabulary.FOLDER_PREFERENCE))
            model.add(pref, model.createProperty(UIVocabulary.FOR), model.createTypedLiteral(folder.id))
            fSettings.save(model, pref)
        }
    }

    fun save(model: Model, names: NameSource, account: IAccount)
    {
        val ui = model.createResource(names.get())
        model.add(ui, model.createProperty(UIVocabulary.RDF_TYPE), model.createResource(UIVocabulary.UI_SETTING))
        model.add(ui, model.createProperty(UIVocabulary.HAS_WIDTH), model.createTypedLiteral(widthProperty.get()))
        model.add(ui, model.createProperty(UIVocabulary.HAS_HEIGHT), model.createTypedLiteral(heightProperty.get()))
        try
        {
            model.add(ui, model.createProperty(UIVocabulary.HAS_VERTICAL_POSITION), model.createTypedLiteral(verticalPosition[0].position))
        } catch (ex: IndexOutOfBoundsException)
        {
            //  ignore
        }

        if (account is IEmailAccount)
        {
            for (folder in account.folders)
            {
                saveFolderSettings(model, ui, names, folder)
            }
        }
    }

    fun getFolderSettings(folder: IFolder): FolderSettings
    {
        val fSettings = folderSettings[folder.id]
        if (fSettings == null)
        {
            folderSettings[folder.id] = FolderSettings()
        }
        return folderSettings[folder.id]!!
    }

    //  accounts view width I guess
    private fun readVerticalPosition(model: Model, name: String)
    {
        val div = Divider()
        div.position = JenaUtils.getD(model, name, UIVocabulary.HAS_VERTICAL_POSITION, DEFAULT_DIVIDER_POSITION)
        verticalPosition.add(div)
    }

    private fun readFolderPrefs(model: Model, name: String)
    {
        val it = JenaUtils.listObjectsOfProperty(model, name, UIVocabulary.HAS_FOLDER_PREFERENCE)
        while (it.hasNext())
        {
            val pref = it.next().asResource()
            val folderId = JenaUtils.getS(JenaUtils.listObjectsOfPropertyU(model, pref.toString(), UIVocabulary.FOR))
            folderSettings[folderId] = FolderSettings.read(model, pref)
        }
    }

    companion object
    {
        const val DEFAULT_DIVIDER_POSITION = 0.15
        const val DEFAULT_WIDTH = 1250
        const val DEFAULT_HEIGHT = 750

        @Throws(IOException::class)
        fun read(fileName: String): UISettings
        {
            val uiModel = ModelFactory.createDefaultModel()
            RDFDataMgr.read(uiModel, Files.newInputStream(Paths.get(OS.getSettingsDir(Babbage::class.java).toString(), fileName)), Lang.TURTLE)

            val settings = UISettings()

            val name: String
            try
            {
                name = JenaUtils.listSubjectsWithPropertyU(uiModel, Vocabulary.RDF_TYPE, UIVocabulary.UI_SETTING).toString()
            } catch (ex: NoSuchElementException)
            {
                //  boot with the defaults
                return settings
            }

            settings.widthProperty.set(JenaUtils.getD(uiModel, name, UIVocabulary.HAS_WIDTH, DEFAULT_WIDTH.toDouble()))
            settings.heightProperty.set(JenaUtils.getD(uiModel, name, UIVocabulary.HAS_HEIGHT, DEFAULT_HEIGHT.toDouble()))

            settings.readVerticalPosition(uiModel, name)

            settings.readFolderPrefs(uiModel, name)

            return settings
        }
    }
}