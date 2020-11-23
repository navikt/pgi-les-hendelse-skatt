package no.nav.pgi.skatt.leshendelse.kafka

import io.prometheus.client.Gauge
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(SekvensnummerConsumer::class.java)

internal class SekvensnummerProducer(kafkaConfig: KafkaConfig) {

    private val sekvensnummerProducer = kafkaConfig.nextSekvensnummerProducer()

    internal fun writeSekvensnummer(sekvensnummer: Long) {
        val record = ProducerRecord(NEXT_SEKVENSNUMMER_TOPIC, "sekvensnummer", sekvensnummer.toString())
        LOG.info("""Adding sekvensnummer "$sekvensnummer" to topic $NEXT_SEKVENSNUMMER_TOPIC""")
        sekvensnummerProducer.send(record).get()
        nextSekvensnummerGauge.set(sekvensnummer.toDouble())
    }

    internal fun close() = sekvensnummerProducer.close()
}

private val nextSekvensnummerGauge = Gauge.build()
        .name("nesteSekvensnummer")
        .help("Neste sekvensnummer som skal brukes når det hentes pgi-hendelser fra skatt").register()