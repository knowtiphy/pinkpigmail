package org.knowtiphy.pinkpigmail.model.imap

import java.util.regex.Pattern

object Patterns
{
    val INBOX_PATTERN: Pattern = Pattern.compile("inbox", Pattern.CASE_INSENSITIVE)
    val TRASH_PATTERN: Pattern = Pattern.compile("trash", Pattern.CASE_INSENSITIVE)
    val JUNK_PATTERN: Pattern = Pattern.compile("spam", Pattern.CASE_INSENSITIVE)
    val SENT_PATTERN: Pattern = Pattern.compile("sent items", Pattern.CASE_INSENSITIVE)
    val DRAFTS_PATTERN: Pattern = Pattern.compile("drafts", Pattern.CASE_INSENSITIVE)
}