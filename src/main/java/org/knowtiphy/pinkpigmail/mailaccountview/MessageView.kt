package org.knowtiphy.pinkpigmail.mailaccountview

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.Label
import javafx.scene.control.MenuButton
import javafx.scene.control.SplitMenuButton
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import org.knowtiphy.pinkpigmail.mailview.MailViewer
import org.knowtiphy.pinkpigmail.model.EmailAddress
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.Format
import org.knowtiphy.pinkpigmail.util.Functions
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.action
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.button
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.resizeable
import org.knowtiphy.utils.HTMLUtils
import org.reactfx.EventStream
import org.w3c.dom.Document

/**
 * @author graham
 */
class MessageView(messageStream : EventStream<IMessage>) : GridPane()
{
	//  this is a hack to "pass" the current message to the webview listener rather than
	//  creating a new webviewer listener per message, and to pass the external refs of a
	//  message to other UI components here
	private var currentMessage : IMessage? = null
	private val externalRefs = FXCollections.observableArrayList<String>()

	private val viewer = resizeable(MailViewer())

	private val fromText = Label()
	private val subjectText = Label()
	private val toText = Label()
	private val headerLeft = GridPane()
	private val receivedOn = Label()

	private val from = Label(Strings.FROM)
	private val to = Label(Strings.TO)
	private val subject = Label(Strings.SUBJECT)

	private val loadRemoteAction =
		action(Icons.loadRemote(), { currentMessage!!.loadRemoteProperty.set(true) }, Strings.LOAD_REMOTE_CONTENT)

	private val trustSenderAction = action(Icons.trustSender(),
		{ currentMessage!!.account.trustSender(currentMessage!!.from) },
		Strings.TRUST_SENDER)

	private val loadRemote = button(loadRemoteAction)
	private val trustSender = button(trustSenderAction)
	private val trustContentMenu = SplitMenuButton()
	private val attachmentsMenu = MenuButton()

	private val buttons = HBox(1.0, attachmentsMenu, trustContentMenu, trustSender, loadRemote)
	private val headerRight = VBox(5.0, buttons, receivedOn)
	private val header = HBox(headerLeft, headerRight)

	private val listener = { _ : ObservableValue<out Document?>, _ : Document?, document : Document? ->
		if (document != null)
		{
			//  can be null when we load to get the initial worker
			currentMessage?.let {
				val message = currentMessage!!
				trustContentMenu.items.clear()
				externalRefs.addAll(HTMLUtils.computeExternalReferences(document))
				for (ref in externalRefs)
				{
					val checkMenuItem = CheckMenuItem(ref)
					checkMenuItem.isSelected = message.account.isTrustedProvider(ref)
					checkMenuItem.selectedProperty().addListener { _, _, newValue ->
						(if (newValue) IEmailAccount::trustProvider else IEmailAccount::unTrustProvider)(message.account,
							ref)
					}
					trustContentMenu.items.add(checkMenuItem)
				}

				trustContentMenu.isDisable = externalRefs.isEmpty()
			}
		}
	}

	init
	{
		addRow(0, header)
		addRow(1, viewer)

		setHgrow(viewer, Priority.ALWAYS)
		setVgrow(viewer, Priority.ALWAYS)
		setHgrow(header, Priority.ALWAYS)
		setVgrow(header, Priority.NEVER)

		HBox.setHgrow(headerLeft, Priority.NEVER)
		HBox.setHgrow(headerRight, Priority.ALWAYS)

		headerRight.alignment = Pos.TOP_RIGHT
		headerLeft.alignment = Pos.TOP_LEFT
		buttons.alignment = Pos.CENTER_RIGHT

		header.background = Background(BackgroundFill(Color.WHITESMOKE, null, null))
		header.padding = Insets(3.0, 3.0, 5.0, 3.0)

		attachmentsMenu.graphic = Icons.attach()
		trustContentMenu.graphic = Icons.trustContentProvider()

		val labelFont = Font.font(from.font.family, FontWeight.BOLD, from.font.size)
		listOf(from, subject, to).forEach { it.font = labelFont }

		with(headerLeft) {
			addColumn(0, from, subject, to)
			addColumn(1, fromText, subjectText, toText)
			hgap = 15.0
			vgap = 5.0
		}

		listOf(from, subject, to, fromText, subjectText, toText).forEach {
			setHgrow(it, Priority.NEVER)
			setVgrow(it, Priority.NEVER)
			it.alignment = Pos.CENTER
		}

		viewer.webView.engine.documentProperty().addListener(listener)
		messageStream.subscribe { newMessage(it) }
	}

	private fun newMessage(message : IMessage)
	{
		//  TODO -- need do remove all bindings from currentMessage (i.e. the previous message)
		//  of have I done that already? Maybe check its complete

		currentMessage = message

		val account = message.folder.account

		account.trustedContentProviders.addListener { _ : ListChangeListener.Change<out String> -> viewer.reload() }
		account.trustedSenders.addListener { _ : ListChangeListener.Change<out EmailAddress> -> viewer.reload() }

		//  disable and clear trustContent menu until the load finishes
		trustContentMenu.items.clear()
		trustContentMenu.isDisable = true

		loadRemoteAction.disabledProperty().unbind()
		loadRemoteAction.isDisabled = true

		trustSenderAction.disabledProperty().unbind()
		trustSenderAction.disabledProperty()
			.bind(Bindings.createBooleanBinding(Functions.callable { account.isTrustedSender(message.from) },
				account.trustedSenders))

		fromText.text = EmailAddress.format(account, message.from)
		subjectText.text = Format.formatN(message.subjectProperty.get())
		toText.text = EmailAddress.format(account, message.to)
		receivedOn.text = Format.asDate(message.receivedOnProperty.get())

		loadRemoteAction.disabledProperty()
			.bind(Bindings.or(Bindings.not(message.loadRemoteProperty), SimpleBooleanProperty(!message.isHTML))
				.or(Bindings.createBooleanBinding(Functions.callable { message.account.isTrustedSender(message.from) },
					message.account.trustedSenders).or(Bindings.isEmpty(externalRefs))))

		val part = message.getContent(account.allowHTMLProperty.get())
		viewer.loadContent(part.content, part.mimeType)
	}
}

//	can we have an exception?
//						catch (exception: Exception)
//						{
//							logger.warning(exception.localizedMessage)
//							viewer.clear()
//							Fail.fail(exception)
//						}
//		catch (ex : Exception)
//		{
//			ex.printStackTrace()
//			//return null;
//		}

//
//  call getAttachments() once because they do database queries so can be slow
//							val attachments = message.attachments
//							if (attachments.isEmpty())
//							{
//								attachmentsMenu.isDisable = true
//							} else
//							{
//								attachmentsMenu.isDisable = false
//								attachmentsMenu.items.clear()
//								Attachments.viewSaveMenu(attachments, attachmentsMenu.items)
//							}
//
//							with(message) {
//								loadRemoteProperty.addListener { _ -> viewer.reload() }
//								loadRemoteProperty.set(loadRemoteProperty.get() || account.isTrustedSender(from))
//							}