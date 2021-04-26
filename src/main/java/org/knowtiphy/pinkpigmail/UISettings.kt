package org.knowtiphy.pinkpigmail

import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.vocabulary.RDF
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.model.IFolder
import org.knowtiphy.utils.JenaUtils
import org.knowtiphy.utils.NameSource
import org.knowtiphy.utils.OS
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author graham
 */
class UISettings
{
	companion object
	{
		@Throws(IOException::class)
		fun read(fileName: String): UISettings
		{
			val model = ModelFactory.createDefaultModel()
			RDFDataMgr.read(
				model,
				Files.newInputStream(Paths.get(OS.getSettingsDir(PinkPigMail::class.java).toString(), fileName)),
				Lang.TURTLE
			)
			//JenaUtils.printModel(model, "READ UI Model")

			val settings = UISettings()

			val uid: String
			try
			{
				uid = JenaUtils.listSubjectsOfType(model, UIVocabulary.UI_SETTING)
			} catch (ex: NoSuchElementException)
			{
				//  boot with the defaults
				return settings
			}

			settings.widthProperty.set(JenaUtils.getD(model, uid, UIVocabulary.HAS_WIDTH, DEFAULT_WIDTH))
			settings.heightProperty.set(JenaUtils.getD(model, uid, UIVocabulary.HAS_HEIGHT, DEFAULT_HEIGHT))

			JenaUtils.listObjectsOfProperty(model, uid, UIVocabulary.HAS_ACCOUNT_SETTINGS).forEach {
				val uaid = it.toString()
				val aid = JenaUtils.getOR(model, uaid, Vocabulary.HAS_ACCOUNT).toString()
				val accountSettings = UIAccountSettings()
				accountSettings.read(model, uaid)
				settings.accountSettings[aid] = accountSettings
			}

			// settings.readFolderPrefs(uiModel, name)


			return settings
		}

		private const val DEFAULT_WIDTH = 1250.0
		private const val DEFAULT_HEIGHT = 750.0
	}

	//  have to set defaults here in case we boot with no UI settings at all
	val widthProperty: DoubleProperty = SimpleDoubleProperty(DEFAULT_WIDTH)
	val heightProperty: DoubleProperty = SimpleDoubleProperty(DEFAULT_HEIGHT)

	private val folderSettings = HashMap<String, UIFolderSettings>()
	private val accountSettings = HashMap<String, UIAccountSettings>()

	private fun saveFolderSettings(model: Model, ui: Resource, names: NameSource, folder: IFolder)
	{
		val fSettings = folderSettings[folder.uri]
		if (fSettings != null)
		{
			val pref = model.createResource(names.get())
			model.add(ui, model.createProperty(UIVocabulary.HAS_FOLDER_PREFERENCE), pref)
			model.add(
				pref,
				model.createProperty(RDF.type.toString()),
				model.createResource(UIVocabulary.FOLDER_PREFERENCE)
			)
			model.add(pref, model.createProperty(UIVocabulary.FOR), model.createTypedLiteral(folder.uri))
			fSettings.save(model, pref)
		}
	}

	fun save(accounts: Collection<IAccount>): Model
	{
		val model = ModelFactory.createDefaultModel()
		model.setNsPrefix("n", Vocabulary.NBASE)
		model.setNsPrefix("o", Vocabulary.TBASE)

		val uid = Globals.nameSource.get()

		JenaUtils.addType(model, uid, UIVocabulary.UI_SETTING)
		JenaUtils.addDP(model, uid, UIVocabulary.HAS_WIDTH, widthProperty.get())
		JenaUtils.addDP(model, uid, UIVocabulary.HAS_HEIGHT, heightProperty.get())

		accounts.forEach { save(model, uid, it) }

		//JenaUtils.printModel(model, "UI Model")

		return model
	}

	fun save(model: Model, uid: String, account: IAccount)
	{
		val uaid = Globals.nameSource.get()

		JenaUtils.addOP(model, uid, UIVocabulary.HAS_ACCOUNT_SETTINGS, uaid)
		JenaUtils.addType(model, uaid, UIVocabulary.UI_ACCOUNT_SETTING)
		JenaUtils.addOP(model, uaid, Vocabulary.HAS_ACCOUNT, account.uri)
		getAccountSettings(account).save(model, uaid)

//        if (account is IEmailAccount)
//        {
//            account.folders.forEach { (_, folder) ->
//                saveFolderSettings(model, ui, names, folder)
//            }
//        }
	}

	fun getAccountSettings(account: IAccount): UIAccountSettings
	{
		if (!accountSettings.containsKey(account.uri))
			accountSettings[account.uri] = UIAccountSettings()
		return accountSettings[account.uri]!!
	}

	fun getFolderSettings(folder: IFolder): UIFolderSettings
	{
		val fSettings = folderSettings[folder.uri]
		if (fSettings == null)
		{
			folderSettings[folder.uri] = UIFolderSettings()
		}
		return folderSettings[folder.uri]!!
	}

//	//  accounts view width I guess
//	private fun readVerticalPosition(model: Model, name: String)
//	{
//		val div = Divider()
//		div.position = JenaUtils.getD(model, name, UIVocabulary.HAS_VERTICAL_POSITION, DEFAULT_DIVIDER_POSITION)
//		verticalPosition.add(div)
//	}

	private fun readFolderPrefs(model: Model, name: String)
	{
		val it = JenaUtils.listObjectsOfProperty(model, name, UIVocabulary.HAS_FOLDER_PREFERENCE)
		while (it.hasNext())
		{
			val pref = it.next().asResource()
			val folderId = JenaUtils.getS(model, pref.toString(), UIVocabulary.FOR)
			folderSettings[folderId] = UIFolderSettings.read(model, pref)
		}
	}
}