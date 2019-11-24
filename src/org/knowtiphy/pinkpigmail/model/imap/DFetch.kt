package org.knowtiphy.pinkpigmail.model.imap

import org.knowtiphy.babbage.storage.Vocabulary

/**
 * @author graham
 */
object DFetch
{
    const val VAR_CID_PART_ID = "cidPartId"
    const val VAR_LOCAL_CID_PART_ID = "localCidPartId"
    const val VAR_ATTACHMENT_ID = "attachmentId"
    const val VAR_FILE_NAME = "fileName"
    const val VAR_MIME_TYPE = "mimeType"

    /**
     * Fetch the CID local names for a message
     *
     * @param messageId
     * @return
     */
    fun cidLocalNames(messageId: String): String
    {
        return String.format(
                "SELECT ?%s ?%s\n"
                        + "WHERE \n"
                        + "{\n"
                        + "      <%s> <%s> <%s>.\n"
                        + "      <%s> <%s> ?%s.\n"
                        + "      ?%s  <%s> ?%s.\n"
                        + "}",
                VAR_CID_PART_ID, VAR_LOCAL_CID_PART_ID,
                messageId, Vocabulary.RDF_TYPE, Vocabulary.IMAP_MESSAGE,
                messageId, Vocabulary.HAS_CID_PART, VAR_CID_PART_ID,
                VAR_CID_PART_ID, Vocabulary.HAS_LOCAL_CID, VAR_LOCAL_CID_PART_ID
        )
    }

    /**
     * Fetch attachments for a message.
     *
     * @param messageId
     * @return
     */
    fun attachments(messageId: String): String
    {
        return String.format(
                "SELECT ?%s ?%s ?%s\n"
                        + "WHERE \n"
                        + "{\n"
                        + "      <%s> <%s> <%s>.\n"
                        + "      <%s> <%s> ?%s.\n"
                        + "      ?%s  <%s> ?%s.\n"
                        + "      ?%s  <%s> ?%s.\n"
                        + "}",
                VAR_ATTACHMENT_ID, VAR_FILE_NAME, VAR_MIME_TYPE,
                messageId, Vocabulary.RDF_TYPE, Vocabulary.IMAP_MESSAGE,
                messageId, Vocabulary.HAS_ATTACHMENT, VAR_ATTACHMENT_ID,
                VAR_ATTACHMENT_ID, Vocabulary.HAS_FILE_NAME, VAR_FILE_NAME,
                VAR_ATTACHMENT_ID, Vocabulary.HAS_MIME_TYPE, VAR_MIME_TYPE)
    }
}