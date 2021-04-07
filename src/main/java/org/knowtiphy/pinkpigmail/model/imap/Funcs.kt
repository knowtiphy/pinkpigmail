package org.knowtiphy.pinkpigmail.model.imap

import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Statement
import org.knowtiphy.pinkpigmail.model.EmailAddress
import org.knowtiphy.utils.JenaUtils

class Funcs
{
    companion object
    {
        val RDF_NODE_TO_EMAIL_ADDRESS: java.util.function.Function<RDFNode, EmailAddress> =
            java.util.function.Function { s: RDFNode -> EmailAddress.create(JenaUtils.getS(s)) }

    }
}