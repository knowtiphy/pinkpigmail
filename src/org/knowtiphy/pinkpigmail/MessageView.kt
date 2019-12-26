package org.knowtiphy.pinkpigmail

import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleBooleanProperty
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
import org.knowtiphy.pinkpigmail.util.Fail
import org.knowtiphy.pinkpigmail.util.Format
import org.knowtiphy.pinkpigmail.util.ui.Replacer
import org.knowtiphy.pinkpigmail.util.ui.UIUtils
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.action
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.button
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.later
import org.knowtiphy.pinkpigmail.util.ui.WaitSpinner
import org.knowtiphy.utils.HTMLUtils
import org.w3c.dom.Document
import java.util.concurrent.ExecutorService
import java.util.logging.Logger

/**
 * @author graham
 */
class MessageView(private val service: ExecutorService,
				  private val messageProperty: ReadOnlyObjectProperty<Pair<IMessage?, Collection<Collection<IMessage>>>>) : Replacer()
{
	private val logger = Logger.getLogger(MessageView::class.qualifiedName)

	private val viewer = UIUtils.resizeable(MailViewer())
	private val noMessageSelected = UIUtils.boxIt(Label(Strings.NO_MESSAGE_SELECTED))
	private val loading = UIUtils.boxIt(WaitSpinner(Strings.LOADING_MESSAGE))

	private val fromText = Label()
	private val subjectText = Label()
	private val toText = Label()
	private val headerLeft = GridPane()
	private val receivedOn = Label()

	private val from = Label(Strings.FROM)
	private val to = Label(Strings.TO)
	private val subject = Label(Strings.SUBJECT)

	private val loadRemoteAction = action(Icons.loadRemote(),
			{
				(messageProperty.get().first ?: return@action).loadRemoteProperty.set(true)
			}, Strings.LOAD_REMOTE_CONTENT)

	private val trustSenderAction = action(Icons.trustSender(),
			{
				val message = messageProperty.get().first ?: return@action
				message.mailAccount.trustSender(message.from)
			}, Strings.TRUST_SENDER)

	private val loadRemote = button(loadRemoteAction)
	private val trustSender = button(trustSenderAction)
	private val trustContentMenu = SplitMenuButton()
	private val attachmentsMenu = MenuButton()

	private val buttons = HBox(attachmentsMenu, trustContentMenu, loadRemote, trustSender)
	private val headerRight = VBox(buttons, receivedOn)
	private val header = HBox(headerLeft, headerRight)
	private val messageSpace = VBox(header, viewer)

	var startTime : Long = 0

	private val listener = { _: ObservableValue<out Document?>, _: Document?, document: Document? ->
		assert(messageProperty.get() != null)
		if (document != null)
		{
			val message = messageProperty.get()!!
			val account = message.first!!.mailAccount
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
			loadRemoteAction.disabledProperty().bind(Bindings.or(message.first!!.loadRemoteProperty,
					SimpleBooleanProperty(!message.first!!.isHTML || externalRefs.isEmpty())).or(
					Bindings.createBooleanBinding(
							UIUtils.callable { account.isTrustedSender(message.first!!.from) }, account.trustedSenders)))
		}
	}

	init
	{
		children.addAll(messageSpace, loading, noMessageSelected)

		attachmentsMenu.graphic = Icons.attach()

		viewer.webView.engine.loadWorker.stateProperty().addListener { _, _, newState: Worker.State ->
			if (newState == Worker.State.SUCCEEDED)
			{
				later {
					println("FLIPPING TO MESSAGE SPACE : " + (System.currentTimeMillis() - startTime))
					flip(messageSpace)
					println("STARTING LOAD AHEADS")
					messageProperty.get().second.forEach { messageProperty.get().first!!.folder.loadAhead(it) }
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
		UIUtils.resizeable(messageSpace)

		viewer.webView.engine.documentProperty().addListener(listener)

		trustContentMenu.graphic = Icons.trustContentProvider()

		messageProperty.addListener { _ -> newMessage() }
	}

	private fun newMessage()
	{
		val message = messageProperty.get().first
		if (message == null)
		{
			flip(noMessageSelected)
		} else
		{
			println("FLIP Loading")
			flip(loading)
			println("STARTING TASK")
			startTime = System.currentTimeMillis()

			val task = object : Task<Void>()
			{
				override fun call(): Void?
				{
					val account = message.mailAccount
					println("Client grabbing content :: " + message.id)
					val part = message.getContent(account.allowHTMLProperty.get())

					later {
					//	val startTime = System.currentTimeMillis()
						try
						{
							//  they may have clicked on a new message in the time between the task was started
							//  and the time at which we get to this point
							if (message == messageProperty.get().first)
							{
								println("Client GOT content :: " + message.id)
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
								receivedOn.text = Format.asDate(message.receivedOnProperty.get())

								println("Client LOAD content :: " + message.id)
								viewer.loadContent(part.content, part.mimeType)

								println("Client LOAD ATTACHMENTS :: " + message.id)
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
									loadRemoteProperty.set(loadRemoteProperty.get() || account.isTrustedSender(from))
								}
							} else
								println("IGNORING MESSAGE")
						}
						catch (exception: Exception)
						{
							logger.warning(exception.localizedMessage)
							viewer.clear()
							Fail.fail(exception)
						}

						println("Client LOAD DONE :: " + (System.currentTimeMillis() - startTime))
					}

					return null
				}
			}

			service.submit(task)
		}
	}
}