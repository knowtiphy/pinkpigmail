package org.knowtiphy.pinkpigmail.model.imap

import org.knowtiphy.babbage.storage.Vocabulary

/**
 * @author graham
 */
object DFetch
{
    val VAR_FOLDER_ID = "folderId"
    val VAR_NAME = "name"
    val VAR_MESSAGE_COUNT = "messageCount"
    val VAR_UNREAD_MESSAGE_COUNT = "unreadMessageCount"

    val VAR_MESSAGE_ID = "messageId"
    val VAR_IS_READ = "isRead"
    val VAR_IS_ANSWERED = "isAnswered"
    val VAR_IS_JUNK = "isJunk"
    val VAR_CONTENT = "content"
    val VAR_SUBJECT = "subject"
    val VAR_TO = "to"
    val VAR_FROM = "from"
    val VAR_CC = "cc"
    val VAR_BCC = "bcc"
    val VAR_SENT_ON = "sentOn"
    val VAR_RECEIVED_ON = "receivedOn"

    val VAR_CID_PART_ID = "cidPartId"
    val VAR_LOCAL_CID_PART_ID = "localCidPartId"
    val VAR_ATTACHMENT_ID = "attachmentId"
    val VAR_FILE_NAME = "fileName"
    val VAR_MIME_TYPE = "mimeType"

    /**
     * Fetch folders in an account
     *
     * @param accountId
     * @param //folderId
     * @return
     */

    //    public static String folders(String accountId)
    //    {
    //        return String.format("SELECT ?%s\n"
    //                        + "WHERE \n"
    //                        + "{\n"
    //                        + "      ?%s <%s> <%s>.\n"
    //                        + "      <%s> <%s> ?%s.\n"
    //                        + "}",
    //                VAR_FOLDER_ID,
    //                VAR_FOLDER_ID, Vocabulary.RDF_TYPE, Vocabulary.IMAP_FOLDER,
    //                accountId, Vocabulary.CONTAINS, VAR_FOLDER_ID);
    //    }

    //    public static String folderDetails(String accountId, String folderId)
    //    {
    //        // Each of those selects is what I need to be constructing something for?
    //        return String.format("SELECT ?%s ?%s ?%s ?%s\n"
    //                        + "WHERE \n"
    //                        + "{\n"
    //                        + "      ?%s <%s> <%s>.\n"
    //                        + "      <%s> <%s> ?%s.\n"
    //                        + "      ?%s <%s> ?%s.\n"
    //                        + "      ?%s <%s> ?%s.\n"
    //                        + "      ?%s <%s> ?%s.\n"
    //                        + "}",
    //                VAR_FOLDER_ID, VAR_NAME, VAR_MESSAGE_COUNT, VAR_UNREAD_MESSAGE_COUNT,
    //                VAR_FOLDER_ID, Vocabulary.RDF_TYPE, Vocabulary.IMAP_FOLDER,
    //                accountId, Vocabulary.CONTAINS, VAR_FOLDER_ID,
    //                VAR_FOLDER_ID, Vocabulary.HAS_NAME, VAR_NAME,
    //                VAR_FOLDER_ID, Vocabulary.HAS_MESSAGE_COUNT, VAR_MESSAGE_COUNT,
    //                VAR_FOLDER_ID, Vocabulary.HAS_UNREAD_MESSAGE_COUNT, VAR_UNREAD_MESSAGE_COUNT);
    //    }
    fun outboxMessage(accountId: String, messageId: String): String
    {
        return String.format(
                "SELECT ?%s ?%s ?%s ?%s ?%s\n"
                        + "WHERE \n"
                        + "{\n"
                        + "      <%s> <%s> <%s>.\n"
                        + "      <%s> <%s> <%s>.\n"
                        + "      OPTIONAL { <%s> <%s> ?%s }\n"
                        + "      OPTIONAL { <%s> <%s> ?%s }\n"
                        + "      OPTIONAL { <%s> <%s> ?%s }\n"
                        + "      OPTIONAL { <%s> <%s> ?%s }\n"
                        + "      OPTIONAL { <%s> <%s> ?%s }\n"
                        + "}",
                VAR_SUBJECT, VAR_TO, VAR_CC, VAR_BCC, VAR_CONTENT,
                messageId, Vocabulary.RDF_TYPE, Vocabulary.DRAFT_MESSAGE,
                accountId, Vocabulary.CONTAINS, messageId,
                messageId, Vocabulary.HAS_SUBJECT, VAR_SUBJECT,
                messageId, Vocabulary.TO, VAR_TO,
                messageId, Vocabulary.HAS_CC, VAR_CC,
                messageId, Vocabulary.HAS_BCC, VAR_BCC,
                messageId, Vocabulary.HAS_CONTENT, VAR_CONTENT)
    }

    /**
     * Fetch message headers for org.knowtiphy.pinkpigmail.messages in a folder.
     *
     * @param folderId
     * @return
     */
    //    public static String messageHeaders(String folderId)
    //    {
    //        return String.format(
    //                "SELECT ?%s ?%s ?%s ?%s ?%s ?%s ?%s ?%s ?%s ?%s ?%s\n"
    //                        + "WHERE \n"
    //                        + "{\n"
    //                        + "      <%s> <%s> ?%s.\n"
    //                        + "      ?%s  <%s> <%s>.\n"
    //                        + "      ?%s  <%s> ?%s.\n"
    //                        + "      ?%s  <%s> ?%s.\n"
    //                        + "      ?%s  <%s> ?%s.\n"
    //                        + "      OPTIONAL { ?%s  <%s> ?%s }\n"
    //                        + "      OPTIONAL { ?%s  <%s> ?%s }\n"
    //                        + "      OPTIONAL { ?%s  <%s> ?%s }\n"
    //                        + "      OPTIONAL { ?%s  <%s> ?%s }\n"
    //                        + "      OPTIONAL { ?%s  <%s> ?%s }\n"
    //                        + "      OPTIONAL { ?%s  <%s> ?%s }\n"
    //                        + "      OPTIONAL { ?%s  <%s> ?%s }\n"
    //                        + "}",
    //                VAR_MESSAGE_ID, VAR_IS_READ, VAR_IS_JUNK, VAR_IS_ANSWERED, VAR_SUBJECT, VAR_TO, VAR_FROM, VAR_CC, VAR_BCC, VAR_RECEIVED_ON, VAR_SENT_ON,
    //                folderId, Vocabulary.CONTAINS, VAR_MESSAGE_ID,
    //                VAR_MESSAGE_ID, Vocabulary.RDF_TYPE, Vocabulary.IMAP_MESSAGE,
    //                VAR_MESSAGE_ID, Vocabulary.IS_READ, VAR_IS_READ,
    //                VAR_MESSAGE_ID, Vocabulary.IS_JUNK, VAR_IS_JUNK,
    //                VAR_MESSAGE_ID, Vocabulary.IS_ANSWERED, VAR_IS_ANSWERED,
    //                VAR_MESSAGE_ID, Vocabulary.HAS_SUBJECT, VAR_SUBJECT,
    //                VAR_MESSAGE_ID, Vocabulary.RECEIVED_ON, VAR_RECEIVED_ON,
    //                VAR_MESSAGE_ID, Vocabulary.SENT_ON, VAR_SENT_ON,
    //                VAR_MESSAGE_ID, Vocabulary.TO, VAR_TO,
    //                VAR_MESSAGE_ID, Vocabulary.FROM, VAR_FROM,
    //                VAR_MESSAGE_ID, Vocabulary.HAS_CC, VAR_CC,
    //                VAR_MESSAGE_ID, Vocabulary.HAS_BCC, VAR_BCC
    //        );
    //    }

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