package org.knowtiphy.pinkpigmail.model.imap

import org.apache.jena.rdf.model.Statement
import org.knowtiphy.pinkpigmail.model.EmailAddress
import org.knowtiphy.utils.JenaUtils

class Funcs
{
    companion object
    {
        val STMT_TO_EMAIL_ADDRESS: java.util.function.Function<Statement, EmailAddress> =
                java.util.function.Function { s: Statement -> EmailAddress.create(JenaUtils.getS(s)) }
    }
}