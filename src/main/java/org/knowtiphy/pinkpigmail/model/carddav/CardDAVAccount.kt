package org.knowtiphy.pinkpigmail.model.caldav

import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.RDF
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.model.storage.DavStorage
import org.knowtiphy.pinkpigmail.model.IContactAccount
import org.knowtiphy.pinkpigmail.util.ui.StorageEvent
import org.knowtiphy.utils.JenaUtils

/**
 * @author graham
 */
class CardDAVAccount(id: String, storage: DavStorage) : StoredPeer<DavStorage>(id, storage), IContactAccount
{
	override val addressBooks: ObservableList<CardDAVAddressBook> = FXCollections.observableArrayList()
	override val nickNameProperty = SimpleStringProperty()
	override val emailAddressProperty = SimpleStringProperty()

	private val serverNameProperty = SimpleStringProperty()
	private val serverHeaderProperty = SimpleStringProperty()
	private val passwordProperty = SimpleStringProperty()

	private val groups = FXCollections.observableArrayList<CardDAVGroup>()

	init
	{
		declareU(Vocabulary.HAS_SERVER_NAME, serverNameProperty)
		declareU(Vocabulary.HAS_SERVER_HEADER, serverHeaderProperty)
		declareU(Vocabulary.HAS_EMAIL_ADDRESS, emailAddressProperty)
		declareU(Vocabulary.HAS_NICK_NAME, nickNameProperty)
		declareU(Vocabulary.HAS_PASSWORD, passwordProperty)
		declareOU(Vocabulary.CONTAINS, addressBooks)
		declareD(Vocabulary.CONTAINS, addressBooks)
		declareOU(Vocabulary.HAS_GROUP, groups)
		declareD(Vocabulary.HAS_GROUP, groups)

		emailAddressProperty.addListener { _: ObservableValue<out String?>, _: String?, newValue: String? ->
			if (nickNameProperty.get() == null)
				nickNameProperty.set(newValue)
		}
	}

	override fun handleEvent(event: StorageEvent)
	{
		println("IN EVENT HANDLER for CardDav")
		println("Event $event")
		JenaUtils.printModel(event.model, "Model $id")
	}

	override fun save(model: Model, name: Resource)
	{
		model.add(name, model.createProperty(RDF.type.toString()), model.createResource(Vocabulary.CALDAV_ACCOUNT))
		model.add(name, model.createProperty(Vocabulary.HAS_SERVER_NAME), serverNameProperty.get())
		model.add(name, model.createProperty(Vocabulary.HAS_SERVER_HEADER), serverHeaderProperty.get())
		model.add(name, model.createProperty(Vocabulary.HAS_EMAIL_ADDRESS), emailAddressProperty.get())
//        model.add(name, model.createProperty(Vocabulary.HAS_PASSWORD), passwordProperty.get())
	}
}