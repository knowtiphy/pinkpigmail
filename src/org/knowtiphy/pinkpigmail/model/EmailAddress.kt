package org.knowtiphy.pinkpigmail.model

import javax.mail.internet.InternetAddress

class EmailAddress(val personal: String?, val address: String) : Comparable<EmailAddress>
{
    companion object
    {
        @JvmStatic
        fun create(address: String): EmailAddress
        {
            //  TODO -- need to write this without using javax.mail.Address
//            val pos = address.indexOf('<')
//            val last = address.lastIndexOf('>')
//
//            val fst = address.substring(if (pos == -1) 0 else pos + 1, if (last == -1) address.length else last).trim { it <= ' ' }
//            val start = if (fst[0] == '"') 1 else 0
//            val end = if (fst[fst.length - 1] == '"') fst.length - 1 else fst.length
//            val res = fst.substring(start, end)
            val internetAddress = InternetAddress(address)
            return EmailAddress(internetAddress.personal, internetAddress.address)
            //return if (account == null) address else if (account.isTrustedSender(listOf(address))) res.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] else res
        }

        @JvmStatic
        fun create(addresses: Collection<String>): Collection<EmailAddress>
        {
            return addresses.map { create(it) }
        }

        @JvmStatic
        fun format(mailAccount: IEmailAccount, addresses: List<EmailAddress>): String
        {
            if (addresses.isEmpty())
            {
                return ""
            }
            val builder = StringBuilder()
            for (i in 0 until addresses.size - 1)
            {
                builder.append(addresses[i].format(mailAccount))
                builder.append(", ")
            }
            builder.append(addresses[addresses.size - 1].format(mailAccount))

            return builder.toString()
        }
    }

    fun format(mailAccount: IEmailAccount) = if (mailAccount.isTrustedSender(listOf(this))) (personal ?: address) else address

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmailAddress

        if (address != other.address) return false

        return true
    }

    override fun hashCode() = address.hashCode()

    override fun compareTo(other: EmailAddress) = address.compareTo(other.address)
}