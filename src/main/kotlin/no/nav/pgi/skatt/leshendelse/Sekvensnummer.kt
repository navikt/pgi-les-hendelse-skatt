package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerConsumer
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerProducer
import no.nav.pgi.skatt.leshendelse.skatt.FirstSekvensnummerClient

private const val FIRST_VALID_SEKVENSNUMMER = 1
private const val NOT_INITIALIZED = -2L

internal class Sekvensnummer(kafkaConfig: KafkaConfig, env: Map<String, String>) {
    private val sekvensnummerConsumer = SekvensnummerConsumer(kafkaConfig)
    private val nextSekvensnummerProducer = SekvensnummerProducer(kafkaConfig)
    private val firstSekvensnummerClient = FirstSekvensnummerClient(env)

    private var currentSekvensnummer = NOT_INITIALIZED

    internal var value
        get():Long {
            if (currentSekvensnummer == NOT_INITIALIZED) {
                currentSekvensnummer = getInitialSekvensnummer()
            }
            return currentSekvensnummer
        }
        set(newSekvensnummer) {
            if (newSekvensnummer >= FIRST_VALID_SEKVENSNUMMER) {
                currentSekvensnummer = newSekvensnummer
                nextSekvensnummerProducer.writeSekvensnummer(newSekvensnummer)
            }
        }

    internal fun close(){
        sekvensnummerConsumer.close()
        nextSekvensnummerProducer.close()
    }

    private fun getInitialSekvensnummer(): Long =
            sekvensnummerConsumer.getNextSekvensnummer()?.toLong() ?: firstSekvensnummerClient.getFirstSekvensnummer()
}