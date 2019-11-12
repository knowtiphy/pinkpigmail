package org.knowtiphy.pinkpigmail

import javafx.application.Platform
import javafx.beans.property.StringProperty
import javafx.concurrent.Task
import javafx.scene.Group
import javafx.scene.Scene
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.knowtiphy.pinkpigmail.model.IAccount
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.util.UIUtils
import org.knowtiphy.utils.*
import java.nio.file.Files
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

/**
 * @author graham
 */
class Shutdown(private val storage: IStorage, private val uiSettings: UISettings)
{
    private var dialog: Pair<Map<IAccount, StringProperty>, Group>? = null
    private val names = NameSource(Vocabulary.NBASE)
    private val accountsModel = createModel()
    private val uiModel = createModel()

    private fun createModel(): Model
    {
        val model = ModelFactory.createDefaultModel()
        model.setNsPrefix("n", Vocabulary.NBASE)
        model.setNsPrefix("o", Vocabulary.TBASE)
        return model
    }

    private fun close(account: IAccount): Thread
    {
        val message = dialog!!.fst()[account] ?: error("")

        return Thread {
            Platform.runLater { message.set("Saving account information") }
            account.save(accountsModel, accountsModel.createResource(names.get()))

            Platform.runLater { message.set("Saving UI settings") }

            Platform.runLater { message.set("Saving account settings") }
            try
            {
                uiSettings.save(uiModel, names, account)
            } catch (ex: Exception)
            {
                Logger.getLogger(PinkPigMail::class.java.name).log(Level.SEVERE, null, ex)
            }

            Platform.runLater { message.set("Closing storage") }
            try
            {
                storage.close()
            } catch (ex: Exception)
            {
                ex.printStackTrace()
                //  ignore
            }
        }
    }

    //	on shutdown save settings and close all folders and accounts
    fun shutdown(primaryStage: Stage, accounts: Collection<IAccount>)
    {
        val stage = UIUtils.stage()
        stage.initStyle(StageStyle.UNDECORATED)
        stage.title = Strings.SHUTTING_DOWN
        stage.initOwner(primaryStage)
        dialog = AccountDialog.create(accounts)
        stage.scene = Scene((dialog ?: return).snd())
        stage.isAlwaysOnTop = true
        stage.centerOnScreen()
        stage.initModality(Modality.APPLICATION_MODAL)

        val task = object : Task<Any?>()
        {
            @Throws(Exception::class)
            override fun call(): Any?
            {
                val threads = LinkedList<Thread>()
                //  trying to find the bug where the accounts are empty
                assert(!accounts.isEmpty())
                for (account in accounts)
                {
                    threads.add(close(account))
                }
                Threads.startAndWait(threads)

                JenaUtils.printModel(accountsModel, "")
                RDFDataMgr.write(Files.newOutputStream(OS.getAppFile(PinkPigMail::class.java, Constants.ACCOUNTS_FILE)), accountsModel, Lang.TURTLE)
                RDFDataMgr.write(Files.newOutputStream(OS.getAppFile(PinkPigMail::class.java, Constants.UI_FILE)), uiModel, Lang.TURTLE)

                //stage.close();
                exitProcess(1)
            }
        }

        Thread(task).start()
        stage.show()
    }
}
