package org.knowtiphy.pinkpigmail

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.Label
import javafx.scene.control.MenuButton
import javafx.scene.control.SplitMenuButton
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import org.knowtiphy.pinkpigmail.mailview.MailViewer
import org.knowtiphy.pinkpigmail.model.EmailAddress
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.*
import org.knowtiphy.utils.HTMLUtils
import org.w3c.dom.Document
import java.util.concurrent.ExecutorService
import java.util.logging.Logger

/**
 * @author graham
 */
class MessageView(private val service: ExecutorService) : Flipper<Number>(SimpleIntegerProperty())
{
    private val logger = Logger.getLogger(MessageView::class.qualifiedName)

    companion object
    {
        private const val MESSAGE = 0
        private const val WAITING = 1
        private const val NONE = 2
    }

    private val messageProperty = SimpleObjectProperty<IMessage?>()

    private val viewer = MailViewer()
    private val noMessageSelected = StackPane()
    private val loadingLabel = Label("Loading Message -- Please Wait")
    private val loading = BorderPane()

    private val fromText = Label()
    private val subjectText = Label()
    private val toText = Label()
    private val headerLeft = GridPane()
    private val receivedOn = Label()

    private val from = Label(Strings.FROM)
    private val to = Label(Strings.TO)
    private val subject = Label(Strings.SUBJECT)

    private val loadRemoteAction = ActionHelper.create(Icons.loadRemote(),
            {
                (messageProperty.get() ?: return@create).loadRemoteProperty.set(true)
            }, Strings.LOAD_REMOTE_CONTENT)

    private val trustSenderAction = ActionHelper.create(Icons.trustSender(),
            {
                val message = messageProperty.get() ?: return@create
                message.mailAccount.trustSender(message.from)
            }, Strings.TRUST_SENDER)

    private val loadRemote = ButtonHelper.regular(loadRemoteAction)
    private val trustSender = ButtonHelper.regular(trustSenderAction)
    private val trustContentMenu = SplitMenuButton()
    private val attachmentsMenu = MenuButton()

    private val buttons = HBox(attachmentsMenu, trustContentMenu, loadRemote, trustSender)
    private val headerRight = VBox(buttons, receivedOn)
    private val header = HBox(headerLeft, headerRight)
    private val messageSpace = VBox(header, viewer)

    private val listener = { _: ObservableValue<out Document?>, _: Document?, document: Document? ->
        assert(messageProperty.get() != null)
        if (document != null)
        {
            val message = messageProperty.get()!!
            val account = message.mailAccount
            trustContentMenu.items.clear()
            val externalRefs = HTMLUtils.computeExternalReferences(document)
            for (ref in externalRefs)
            {
                val checkMenuItem = CheckMenuItem(ref)
                checkMenuItem.isSelected = account.isTrustedProvider(ref)
                checkMenuItem.selectedProperty().addListener { _, _, newValue ->
                    (if (newValue) IEmailAccount::trustProvider else IEmailAccount::unTrustProvider)(account, ref)
                }
                trustContentMenu.items.add(checkMenuItem)
            }

            trustContentMenu.isDisable = externalRefs.isEmpty()
            loadRemoteAction.disabledProperty().bind(Bindings.or(message.loadRemoteProperty,
                    SimpleBooleanProperty(!message.isHTML || externalRefs.isEmpty())).or(
                    Bindings.createBooleanBinding(
                            UIUtils.callable { account.isTrustedSender(message.from) }, account.trustedSenders)))
        }
    }

    init
    {
        addNode(MESSAGE, messageSpace)
        addNode(WAITING, loading)
        addNode(NONE, noMessageSelected)
        whichProperty.value = NONE

        loading.center = loadingLabel

        //  configure no message selected
        noMessageSelected.background = Background(BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets(0.0)))
        UIUtils.resizable(noMessageSelected)

        //loading.background = Background(BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets(0.0)))
        UIUtils.resizable(loading)

        attachmentsMenu.graphic = Icons.attach()

        UIUtils.resizable(viewer)
        viewer.webView.engine.loadWorker.stateProperty().addListener { _, _, newState: Worker.State ->
            if (newState == Worker.State.SUCCEEDED)
            {
                Platform.runLater {
                    whichProperty.value = MESSAGE
                    //messageSpace.toFront()
                }
            }
        }

        val labelFont = Font.font(from.font.family, FontWeight.BOLD, from.font.size)
        arrayOf(from, subject, to).forEach { it.font = labelFont }

        headerLeft.addColumn(0, from, subject, to)
        headerLeft.addColumn(1, fromText, subjectText, toText)
        for (label in arrayOf(from, subject, to, fromText, subjectText, toText))
        {
            GridPane.setHgrow(label, Priority.NEVER)
            GridPane.setVgrow(label, Priority.NEVER)
            label.alignment = Pos.CENTER
        }

        headerLeft.hgap = 15.0
        headerLeft.vgap = 5.0

        buttons.spacing = 1.0
        buttons.alignment = Pos.CENTER_RIGHT

        headerRight.spacing = 5.0
        headerRight.alignment = Pos.TOP_RIGHT

        HBox.setHgrow(headerLeft, Priority.NEVER)
        HBox.setHgrow(headerRight, Priority.ALWAYS)

        header.background = Background(BackgroundFill(Color.WHITESMOKE, null, null))
        header.padding = Insets(3.0, 3.0, 5.0, 3.0)

        VBox.setVgrow(header, Priority.NEVER)
        VBox.setVgrow(viewer, Priority.ALWAYS)
        UIUtils.resizable(messageSpace)

        viewer.webView.engine.documentProperty().addListener(listener)

        trustContentMenu.graphic = Icons.trustContentProvider()

        messageProperty.addListener { _ -> newMessage() }
    }

    fun setMessage(message: IMessage)
    {
        messageProperty.set(message)
    }

    private fun newMessage()
    {
        val message = messageProperty.get()
        if (message != null)
        {
            whichProperty.value = WAITING
            val task = object : Task<Void>()
            {
                override fun call(): Void?
                {
                    val account = message.mailAccount
                    println("getting content")

                    val part = message.getContent(account.allowHTMLProperty.get())
                    println("content")
                    println(part)

                    Platform.runLater {
                        try
                        {
                            //  disable and clear trustContent menu until the load finishes
                            trustContentMenu.items.clear()
                            trustContentMenu.isDisable = true

                            loadRemoteAction.disabledProperty().unbind()
                            loadRemoteAction.isDisabled = true

                            trustSenderAction.disabledProperty().unbind()
                            trustSenderAction.disabledProperty().bind(Bindings.createBooleanBinding(
                                    UIUtils.callable { account.isTrustedSender(message.from) }, account.trustedSenders))

                            fromText.text = EmailAddress.format(message.mailAccount, message.from)
                            subjectText.text = Format.formatN(message.subjectProperty.get())
                            toText.text = EmailAddress.format(message.mailAccount, message.to)
                            receivedOn.text = Format.format(message.receivedOnProperty.get())

                            try
                            {
                                viewer.loadContent(part.content, part.mimeType)

                                //  call getAttachments() once because they do database queries so can be slow
                                val attachments = message.attachments
                                if (attachments.isEmpty())
                                {
                                    attachmentsMenu.isDisable = true
                                } else
                                {
                                    attachmentsMenu.isDisable = false
                                    attachmentsMenu.items.clear()
                                    Attachments.viewSaveMenu(attachments, attachmentsMenu.items)
                                }

                                with(message) {
                                    loadRemoteProperty.addListener { _ -> viewer.reload() }
                                    account.trustedContentProviders.addListener { _: ListChangeListener.Change<out String> -> viewer.reload() }
                                    account.trustedSenders.addListener { _: ListChangeListener.Change<out EmailAddress> -> viewer.reload() }
                                    loadRemoteProperty.set(message.loadRemoteProperty.get() || account.isTrustedSender(message.from))
                                }
                            } catch (ex: Exception)
                            {
                                //  TODO -- do something nicer here
                                logger.warning(ex.localizedMessage)
                                viewer.clear()
                                Fail.fail(ex)
                            }
                        } catch (ex: Exception)
                        {
                            logger.warning(ex.localizedMessage)
                        }
                    }

                    return null
                }
            }

            service.submit(task)
        }
    }
}