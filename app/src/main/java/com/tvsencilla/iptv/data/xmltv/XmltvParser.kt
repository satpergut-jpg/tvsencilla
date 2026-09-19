package com.tvsencilla.iptv.data.xmltv

import com.tvsencilla.iptv.domain.model.EpgProgram
import java.io.InputStream
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/**
 * Streams an XMLTV guide with SAX, handing every programme to a callback. A national guide is
 * easily 100+ MB, so nothing larger than one programme is ever held in memory.
 *
 * SAX (rather than XmlPullParser) keeps this class runnable on a plain JVM, which is what makes
 * it unit testable.
 */
class XmltvParser @Inject constructor() {

    fun parse(input: InputStream, onProgram: (EpgProgram) -> Unit) {
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        }
        factory.newSAXParser().parse(input, Handler(onProgram))
    }

    private class Handler(private val onProgram: (EpgProgram) -> Unit) : DefaultHandler() {

        private var channelId: String? = null
        private var startMillis: Long? = null
        private var endMillis: Long? = null
        private var title: StringBuilder? = null
        private var description: StringBuilder? = null
        private var capturing: StringBuilder? = null

        override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
            when (qName?.lowercase()) {
                "programme" -> {
                    channelId = attributes?.getValue("channel")?.trim()
                    startMillis = parseTime(attributes?.getValue("start"))
                    endMillis = parseTime(attributes?.getValue("stop"))
                    title = null
                    description = null
                }
                // Only the first title and desc are kept; guides repeat them per language.
                "title" -> if (channelId != null && title == null) {
                    title = StringBuilder().also { capturing = it }
                }
                "desc" -> if (channelId != null && description == null) {
                    description = StringBuilder().also { capturing = it }
                }
            }
        }

        override fun characters(ch: CharArray?, start: Int, length: Int) {
            val target = capturing ?: return
            if (ch != null) target.appendRange(ch, start, start + length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            when (qName?.lowercase()) {
                "title", "desc" -> capturing = null
                "programme" -> {
                    emit()
                    channelId = null
                    startMillis = null
                    endMillis = null
                    title = null
                    description = null
                    capturing = null
                }
            }
        }

        private fun emit() {
            val id = channelId ?: return
            val start = startMillis ?: return
            val end = endMillis ?: return
            val name = title?.toString()?.trim().orEmpty()
            if (name.isEmpty() || end <= start) return
            onProgram(
                EpgProgram(
                    epgChannelId = id,
                    title = name,
                    startMillis = start,
                    endMillis = end,
                    description = description?.toString()?.trim()?.ifEmpty { null },
                ),
            )
        }
    }

    companion object {
        /**
         * XMLTV timestamps look like "20240115203000 +0100". The offset is optional and some
         * guides shorten the field to minutes or even days, so the digits are padded.
         */
        fun parseTime(value: String?): Long? {
            val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val digits = raw.takeWhile { it.isDigit() }
            if (digits.length < 8) return null
            val padded = digits.padEnd(14, '0')

            val offsetPart = raw.drop(digits.length).trim()
            val offsetMinutes = parseOffsetMinutes(offsetPart) ?: 0

            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    padded.substring(0, 4).toInt(),
                    padded.substring(4, 6).toInt() - 1,
                    padded.substring(6, 8).toInt(),
                    padded.substring(8, 10).toInt(),
                    padded.substring(10, 12).toInt(),
                    padded.substring(12, 14).toInt(),
                )
            }
            return calendar.timeInMillis - offsetMinutes * 60_000L
        }

        private fun parseOffsetMinutes(offset: String): Int? {
            if (offset.length < 5) return null
            val sign = when (offset[0]) {
                '+' -> 1
                '-' -> -1
                else -> return null
            }
            val hours = offset.substring(1, 3).toIntOrNull() ?: return null
            val minutes = offset.substring(3, 5).toIntOrNull() ?: return null
            return sign * (hours * 60 + minutes)
        }
    }
}
