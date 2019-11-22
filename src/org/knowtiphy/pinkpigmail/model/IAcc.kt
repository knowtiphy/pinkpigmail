package org.knowtiphy.pinkpigmail.model

import javafx.beans.property.StringProperty
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.knowtiphy.owlorm.javafx.IPeer

interface IAcc : IPeer
{
    fun save(model: Model, name: Resource)

    val emailAddressProperty: StringProperty

}