package no.nav.pgi.skatt.leshendelse

import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerConsumer
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerProducer
import no.nav.pgi.skatt.leshendelse.skatt.FirstSekvensnummerClient
import no.nav.pgi.skatt.leshendelse.util.getVal
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class Sekvensnummer(
    private val counters: Counters,
    kafkaFactory: KafkaFactory,
    env: Map<String, String>,
) {
    private val nextSekvensnummerProducer = SekvensnummerProducer(
        counters = counters,
        sekvensnummerProducer = kafkaFactory.nextSekvensnummerProducer()
    )
    private val firstSekvensnummer: Long by lazy { getInitialSekvensnummer(kafkaFactory, env) }
    private var currentSekvensnummer = NOT_INITIALIZED

    internal fun getSekvensnummer(): Long {
        if (currentSekvensnummer == NOT_INITIALIZED) setSekvensnummer(firstSekvensnummer)
        return currentSekvensnummer
    }

    internal fun setSekvensnummer(sekvensnummer: Long) {
        if (sekvensnummer > USE_PREVIOUS) {
            addSekvensnummerToTopic(sekvensnummer)
            currentSekvensnummer = sekvensnummer
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
    val consumer = SekvensnummerConsumer(kafkaFactory.nextSekvensnummerConsumer())
    val client = FirstSekvensnummerClient(env)
    val tilbakestillSekvensnummer = TilbakestillSekvensnummer(env)
    val sekvensnummer = if (tilbakestillSekvensnummer.skalTilbakestille()) {
        client.getSekvensnummer(tilbakestillSekvensnummer.hentFra())
    } else {
        consumer.getNextSekvensnummer()?.toLong() ?: client.getSekvensnummer(HentSekvensnummer.FørsteMulige)
    }
    consumer.close()
    return sekvensnummer
}


sealed class HentSekvensnummer {
    data object FørsteMulige : HentSekvensnummer()
    data class FraDato(val date: LocalDate) : HentSekvensnummer()
}

class TilbakestillSekvensnummer(
    env: Map<String, String> = System.getenv()
) {
    private val tilbakestill: Boolean = env.getVal("TILBAKESTILL_SEKVENSNUMMER", "false").toBoolean()
    private val hentFra: String = env.getVal("TILBAKESTILL_SEKVENSNUMMER_TIL", FIRST)

    companion object {
        private const val FIRST = "first"
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    fun skalTilbakestille() = tilbakestill
    fun hentFra(): HentSekvensnummer {
        require(tilbakestill) { "Tilbakestilling av sekvensnummer er ikke aktivert" }
        return when (hentFra == FIRST) {
            true -> {
                log.info("Tilbakestilling av sekvensnummer aktivert - tilbakestiller til første mulige sekvensnummer.")
                HentSekvensnummer.FørsteMulige
            }

            false -> {
                val dato = LocalDate.parse(hentFra)
                log.info("Tilbakestilling av sekvensnummer aktivert - tilbakestiller til første sekvensnummer for dato:$dato")
                HentSekvensnummer.FraDato(LocalDate.parse(hentFra))
            }
        }
    }
}