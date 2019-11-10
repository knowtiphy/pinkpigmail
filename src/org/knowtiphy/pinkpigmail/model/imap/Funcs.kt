package org.knowtiphy.pinkpigmail.model.imap

import org.apache.jena.rdf.model.Statement
import org.knowtiphy.owlorm.javafx.Functions
import org.knowtiphy.pinkpigmail.model.EmailAddress

class Funcs
{
    companion object
    {
        val STMT_TO_EMAIL_ADDRESS: java.util.function.Function<Statement, EmailAddress> =
                java.util.function.Function()
                { s: Statement -> EmailAddress.create(Functions.STMT_TO_STRING.apply(s)) }
    }
}