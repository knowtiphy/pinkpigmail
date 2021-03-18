package org.knowtiphy.pinkpigmail.mailaccountview

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
import org.knowtiphy.pinkpigmail.Attachments
import org.knowtiphy.pinkpigmail.mailview.MailViewer
import org.knowtiphy.pinkpigmail.model.EmailAddress
import org.knowtiphy.pinkpigmail.model.IEmailAccount
import org.knowtiphy.pinkpigmail.model.IMessage
import org.knowtiphy.pinkpigmail.model.IPart
import org.knowtiphy.pinkpigmail.resources.Icons
import org.knowtiphy.pinkpigmail.resources.Strings
import org.knowtiphy.pinkpigmail.util.Format
import org.knowtiphy.pinkpigmail.util.Functions
import org.knowtiphy.pinkpigmail.util.ui.Replacer
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.action
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.boxIt
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.button
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.later
import org.knowtiphy.pinkpigmail.util.ui.UIUtils.resizeable
import org.knowtiphy.pinkpigmail.util.ui.WaitSpinner
import org.knowtiphy.utils.HTMLUtils
import org.w3c.dom.Document
import java.util.concurrent.ExecutorService

/**
 * @author graham
 */
class MessageView(private val account: IEmailAccount, private val service: ExecutorService,
				  private val messageProperty: ReadOnlyObjectProperty<IMessage>) : GridPane()
{
	private val viewer = resizeable(MailViewer())
	private val noMessageSelected = boxIt(Label(Strings.NO_MESSAGE_SELECTED))
	private val loadingSpinner = boxIt(WaitSpinner(Strings.LOADING_MESSAGE))

	//	private val loading = boxIt(loadingSpinner)
	private val foo = Replacer()

	private val fromText = Label()
	private val subjectText = Label()
	private val toText = Label()
	private val headerLeft = GridPane()
	private val receivedOn = Label()

	private val from = Label(Strings.FROM)
	private val to = Label(Strings.TO)
	private val subject = Label(Strings.SUBJECT)

	private val loadRemoteAction = action(Icons.loadRemote(), { messageProperty.get().loadRemoteProperty.set(true) }, Strings.LOAD_REMOTE_CONTENT)

	private val trustSenderAction = action(Icons.trustSender(), { account.trustSender(messageProperty.get().from) }, Strings.TRUST_SENDER)

	private val loadRemote = button(loadRemoteAction)
	private val trustSender = button(trustSenderAction)
	private val trustContentMenu = SplitMenuButton()
	private val attachmentsMenu = MenuButton()

	private val buttons = HBox(1.0, attachmentsMenu, trustContentMenu, loadRemote, trustSender)
	private val headerRight = VBox(5.0, buttons, receivedOn)
	private val header = HBox(headerLeft, headerRight)
	private val messageSpace = resizeable(VBox(header, viewer))

	var startTime: Long = 0

	private val listener = { _: ObservableValue<out Document?>, _: Document?, document: Document? ->
//		assert(messageProperty.get() != null)
		if (messageProperty.get() != null && document != null)
		{
			val message = messageProperty.get()
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
							Functions.callable { account.isTrustedSender(message.from) }, account.trustedSenders)))
		}
	}

	init
	{
		//children.addAll(boxIt(viewer))//, loading, noMessageSelected)
		addRow(0, foo)
		alignment = Pos.CENTER

		foo.flip(noMessageSelected)
		setHgrow(viewer, Priority.ALWAYS)
		setVgrow(viewer, Priority.ALWAYS)
		attachmentsMenu.graphic = Icons.attach()

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

		headerRight.alignment = Pos.TOP_RIGHT

		HBox.setHgrow(headerLeft, Priority.NEVER)
		HBox.setHgrow(headerRight, Priority.ALWAYS)

		header.background = Background(BackgroundFill(Color.WHITESMOKE, null, null))
		header.padding = Insets(3.0, 3.0, 5.0, 3.0)
		VBox.setVgrow(header, Priority.NEVER)
		VBox.setVgrow(viewer, Priority.ALWAYS)

		buttons.alignment = Pos.CENTER_RIGHT

		trustContentMenu.graphic = Icons.trustContentProvider()

		messageProperty.addListener { _, oldValue, newValue ->
			//	TODO -- not sure this explanation makes sense but it does stop the flashing scenario
			//	stops message flashing when a delete causes the selection to change but the message itself  hasn't changed
			//	(just the index of the current message in the list has changed)
			if (newValue != null && (oldValue == null || newValue != oldValue))
			{
				newMessage()
			}
		}

		viewer.webView.engine.documentProperty().addListener(listener)

//		viewer.webView.engine.loadWorker.stateProperty().addListener { _, _, newState: Worker.State ->
//			if (newState == Worker.State.SUCCEEDED)
//			{
//				later {
//					println("FLIPPING TO MESSAGE SPACE : " + (System.currentTimeMillis() - startTime))
//					//loadingSpinner.finish()
//					//flip(messageSpace)
//					//AccountViewModel.messageShown.push(messageProperty.get())
//				}
//			}
//		}

		account.trustedContentProviders.addListener { _: ListChangeListener.Change<out String> -> viewer.reload() }
		account.trustedSenders.addListener { _: ListChangeListener.Change<out EmailAddress> -> viewer.reload() }
	}

	private fun newMessage()
	{
		val message = messageProperty.get()
		if (message == null)
		{
			//	flip(noMessageSelected)
		} else
		{
			println("FLIP Loading")
			//loadingSpinner.resume()
			//foo.flip(loadingSpinner)
			//flip(loading)
			println("STARTING TASK")
			startTime = System.currentTimeMillis()

			val task = object : Task<Void>()
			{
				override fun call(): Void?
				{
					try
					{
						println("Client grabbing content :: " + message.id)
						val part = message.getContent(account.allowHTMLProperty.get())
						//val part = "FOOOO FOOOO FOOOO";
						println("Client GOT content :: " + message.id)

						later {
							//	val startTime = System.currentTimeMillis()
//						try
//						{
							//  they may have clicked on a new message in the time between the task was started
							//  and the time at which we get to this point
							if (message == messageProperty.get())
							{
								//  disable and clear trustContent menu until the load finishes
//							trustContentMenu.items.clear()
//							trustContentMenu.isDisable = true
//
//							loadRemoteAction.disabledProperty().unbind()
//							loadRemoteAction.isDisabled = true
//
//							trustSenderAction.disabledProperty().unbind()
//							trustSenderAction.disabledProperty().bind(Bindings.createBooleanBinding(
//									Functions.callable { account.isTrustedSender(message.from) }, account.trustedSenders))
//
//							fromText.text = EmailAddress.format(account, message.from)
//							subjectText.text = Format.formatN(message.subjectProperty.get())
//							toText.text = EmailAddress.format(account, message.to)
//							receivedOn.text = Format.asDate(message.receivedOnProperty.get())

								println("Client LOAD content :: " + message.id)
								//viewer.loadContent(part.content, part.mimeType )"<html><body>FOOOO FOOOO FOOOO</body></html>", "text/plain")//)
								try
								{
									//viewer.loadContent(part, "text/plain")//)
									viewer.loadContent(part.content, part.mimeType)
								}
								catch (ex: Exception)
								{
									ex.printStackTrace()
								}
								println("Client LOAD ATTACHMENTS :: " + message.id)
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
							} else
							{
								//loadingSpinner.finish()
								//AccountViewModel.messageShown.push(message)
								println("IGNORING MESSAGE")
							}
//						}
							//	can we have an exception?
//						catch (exception: Exception)
//						{
//							logger.warning(exception.localizedMessage)
//							viewer.clear()
//							Fail.fail(exception)
//						}

							println("Client LOAD DONE :: " + (System.currentTimeMillis() - startTime))
							later { foo.flip(messageSpace) }
						}
					}
					catch (ex: Exception)
					{
						ex.printStackTrace()
						//return null;
					}

					return null
				}
			}

			//Thread(task).start();

			service.submit(task)
		}
	}
}
