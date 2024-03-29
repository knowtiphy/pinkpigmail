package org.knowtiphy.pinkpigmail.model.caldav

import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.pinkpigmail.model.BaseAccount
import org.knowtiphy.pinkpigmail.model.IContactAccount

/**
 * @author graham
 */
class CardDAVAccount(id: String, storage: IStorage) : BaseAccount(id, Vocabulary.CARDDAV_ACCOUNT, storage), IContactAccount
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
		addUpdater(Vocabulary.HAS_SERVER_NAME, serverNameProperty)
		addUpdater(Vocabulary.HAS_SERVER_HEADER, serverHeaderProperty)
		addUpdater(Vocabulary.HAS_EMAIL_ADDRESS, emailAddressProperty)
		addUpdater(Vocabulary.HAS_NICK_NAME, nickNameProperty)
		addUpdater(Vocabulary.HAS_PASSWORD, passwordProperty)
//		declareOU(Vocabulary.CONTAINS, addressBooks)
//		declareD(Vocabulary.CONTAINS, addressBooks)
//		declareOU(Vocabulary.HAS_GROUP, groups)
//		declareD(Vocabulary.HAS_GROUP, groups)

		emailAddressProperty.addListener { _: ObservableValue<out String?>, _: String?, newValue: String? ->
			if (nickNameProperty.get() == null)
				nickNameProperty.set(newValue)
		}
	}

	override fun initialize()
	{
		TODO("Not yet implemented")
	}

//	override fun save(model: Model, name: Resource)
//	{
//		model.add(name, model.createProperty(RDF.type.toString()), model.createResource(Vocabulary.CALDAV_ACCOUNT))
//		model.add(name, model.createProperty(Vocabulary.HAS_SERVER_NAME), serverNameProperty.get())
//		model.add(name, model.createProperty(Vocabulary.HAS_SERVER_HEADER), serverHeaderProperty.get())
//		model.add(name, model.createProperty(Vocabulary.HAS_EMAIL_ADDRESS), emailAddressProperty.get())
////        model.add(name, model.createProperty(Vocabulary.HAS_PASSWORD), passwordProperty.get())
//	}
}