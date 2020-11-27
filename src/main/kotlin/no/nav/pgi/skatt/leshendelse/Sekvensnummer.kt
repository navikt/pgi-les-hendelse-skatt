package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerConsumer
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerProducer
import no.nav.pgi.skatt.leshendelse.skatt.FirstSekvensnummerClient
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(Sekvensnummer::class.java.simpleName)

internal class Sekvensnummer(kafkaFactory: KafkaFactory, env: Map<String, String>) {
    private val sekvensnummerConsumer = SekvensnummerConsumer(kafkaFactory)
    private val nextSekvensnummerProducer = SekvensnummerProducer(kafkaFactory)
    private val firstSekvensnummerClient = FirstSekvensnummerClient(env)
    private var currentSekvensnummer = NOT_INITIALIZED

    internal var value
        get():Long {
            if (currentSekvensnummer == NOT_INITIALIZED) value = getInitialSekvensnummer()
            return currentSekvensnummer
        }
        set(newSekvensnummer) {
            when {
                newSekvensnummer <= USE_PREVIOUS -> {
                    LOG.info("""New sekvensnummer was not set because it was equal or less then $USE_PREVIOUS""")
                }
                newSekvensnummer <= currentSekvensnummer -> {
                    LOG.info("""New sekvensnummer was not set because it was equal or less then current sekvensnummer """)
                }
                else -> {
                    addSekvensnummerToTopic(newSekvensnummer)
                    currentSekvensnummer = newSekvensnummer
                }
            }
        }

    private fun getInitialSekvensnummer(): Long = sekvensnummerConsumer.getNextSekvensnummer()?.toLong()
            ?: firstSekvensnummerClient.getFirstSekvensnummer()

    internal fun addSekvensnummerToTopic(sekvensnummer: Long, synchronous: Boolean = false) {
        nextSekvensnummerProducer.writeSekvensnummer(sekvensnummer, synchronous)
    }

    internal fun close() {
        sekvensnummerConsumer.close()
        nextSekvensnummerProducer.close()
    }

    companion object {
        internal const val NOT_INITIALIZED = -9999L
        internal const val USE_PREVIOUS = -1L
    }
}