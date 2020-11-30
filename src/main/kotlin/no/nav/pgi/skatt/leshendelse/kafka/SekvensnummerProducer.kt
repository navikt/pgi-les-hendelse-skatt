package no.nav.pgi.skatt.leshendelse.kafka

import io.prometheus.client.Gauge
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(SekvensnummerConsumer::class.java)

internal class SekvensnummerProducer(kafkaFactory: KafkaFactory) {
    private val sekvensnummerProducer = kafkaFactory.nextSekvensnummerProducer()

    internal fun writeSekvensnummer(sekvensnummer: Long, synchronous: Boolean = false) {
        val record = ProducerRecord(NEXT_SEKVENSNUMMER_TOPIC, "sekvensnummer", sekvensnummer.toString())
        if (synchronous) {
            sekvensnummerProducer.send(record)
            logAddedSekvensnummer(record)
        } else {
            sekvensnummerProducer.send(record, callBack(record))
        }
    }

    internal fun close() = sekvensnummerProducer.close().also { LOG.info("sekvensnummerProducer closed") }

    private fun callBack(record: ProducerRecord<String, String>): (metadata: RecordMetadata, exception: Exception?) -> Unit =
            { recordMetadata, exception ->
                if (exception == null) {
                    logAddedSekvensnummer(record)
                } else {
                    LOG.error("""Error while sending sekvensnummer "${record.value()}" """)
                    throw exception
                }
            }

    private fun logAddedSekvensnummer(record: ProducerRecord<String, String>) {
        LOG.info("""Added sekvensnummer ${record.value()} to topic ${record.topic()}""")
        persistedSekvensnummerGauge.set(record.value().toDouble())
    }
}

private val persistedSekvensnummerGauge = Gauge.build()
        .name("persistedSekvensnummer")
        .help("Siste persisterte som brukes når det hentes pgi-hendelser fra skatt")
        .register()