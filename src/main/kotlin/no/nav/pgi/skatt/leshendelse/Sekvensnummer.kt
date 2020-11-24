package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerConsumer
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerProducer
import no.nav.pgi.skatt.leshendelse.skatt.FirstSekvensnummerClient

internal class Sekvensnummer(kafkaFactory: KafkaFactory, env: Map<String, String>) {
    private val sekvensnummerConsumer = SekvensnummerConsumer(kafkaFactory)
    private val nextSekvensnummerProducer = SekvensnummerProducer(kafkaFactory)
    private val firstSekvensnummerClient = FirstSekvensnummerClient(env)

    private var currentSekvensnummer = NOT_INITIALIZED

    internal var value
        get():Long {
            if (currentSekvensnummer == NOT_INITIALIZED) setSekvensnummer(getInitialSekvensnummer())
            return currentSekvensnummer
        }
        set(newSekvensnummer) {
            if (newSekvensnummer > USE_PREVIOUS && newSekvensnummer > currentSekvensnummer)
                setSekvensnummer(newSekvensnummer)
        }

    private fun getInitialSekvensnummer(): Long =
            sekvensnummerConsumer.getNextSekvensnummer()?.toLong() ?: firstSekvensnummerClient.getFirstSekvensnummer()

    internal fun setSekvensnummer(sekvensnummer: Long, synchronous: Boolean = false) {
        currentSekvensnummer = sekvensnummer
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