package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerConsumer
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerProducer
import no.nav.pgi.skatt.leshendelse.skatt.FirstSekvensnummerClient
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(Sekvensnummer::class.java.simpleName)

internal class Sekvensnummer(kafkaFactory: KafkaFactory, env: Map<String, String>) {
    private val nextSekvensnummerProducer = SekvensnummerProducer(kafkaFactory)
    private val firstSekvensnummer: Long by lazy { getInitialSekvensnummer(kafkaFactory, env) }
    private var currentSekvensnummer = NOT_INITIALIZED

    internal fun getSekvensnummer(): Long {
        if (currentSekvensnummer == NOT_INITIALIZED) setSekvensnummer(firstSekvensnummer)
        return currentSekvensnummer
    }

    internal fun setSekvensnummer(sekvensnummer: Long) {
        when {
            sekvensnummer <= USE_PREVIOUS -> {
                LOG.info("""New sekvensnummer was not set because it was equal or less then $USE_PREVIOUS""")
            }
            else -> {
                addSekvensnummerToTopic(sekvensnummer)
                currentSekvensnummer = sekvensnummer
            }
        }
    }

    internal fun addSekvensnummerToTopic(sekvensnummer: Long, synchronous: Boolean = false) {
        nextSekvensnummerProducer.writeSekvensnummer(sekvensnummer, synchronous)
    }

    internal fun close() {
        nextSekvensnummerProducer.close()
    }

    companion object {
        internal const val NOT_INITIALIZED = -9999L
        internal const val USE_PREVIOUS = -1L
    }
}

private fun getInitialSekvensnummer(kafkaFactory: KafkaFactory, env: Map<String, String>): Long {
    val consumer = SekvensnummerConsumer(kafkaFactory)
    val client = FirstSekvensnummerClient(env)
    val sekvensnummer = consumer.getNextSekvensnummer()?.toLong() ?: client.getFirstSekvensnummer()
    consumer.close()
    return sekvensnummer
}