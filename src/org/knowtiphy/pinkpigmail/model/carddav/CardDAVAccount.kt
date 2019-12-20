package org.knowtiphy.pinkpigmail.model.caldav

import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.knowtiphy.babbage.storage.IStorage
import org.knowtiphy.babbage.storage.Vocabulary
import org.knowtiphy.owlorm.javafx.StoredPeer
import org.knowtiphy.pinkpigmail.model.IContactAccount

/**
 * @author graham
 */
class CardDAVAccount(id: String, storage: IStorage) : StoredPeer(id, storage), IContactAccount
{
    override val addressBooks : ObservableList<CardDAVAddressBook> = FXCollections.observableArrayList()
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

    override fun save(model: Model, name: Resource)
    {
        model.add(name, model.createProperty(Vocabulary.RDF_TYPE), model.createResource(Vocabulary.CALDAV_ACCOUNT))
        model.add(name, model.createProperty(Vocabulary.HAS_SERVER_NAME), serverNameProperty.get())
        model.add(name, model.createProperty(Vocabulary.HAS_SERVER_HEADER), serverHeaderProperty.get())
        model.add(name, model.createProperty(Vocabulary.HAS_EMAIL_ADDRESS), emailAddressProperty.get())
//        model.add(name, model.createProperty(Vocabulary.HAS_PASSWORD), passwordProperty.get())
    }
}