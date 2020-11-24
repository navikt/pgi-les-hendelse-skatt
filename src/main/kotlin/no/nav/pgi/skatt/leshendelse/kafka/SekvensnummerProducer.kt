package no.nav.pgi.skatt.leshendelse.kafka

import io.prometheus.client.Gauge
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(SekvensnummerConsumer::class.java)

internal class SekvensnummerProducer(kafkaConfig: KafkaConfig) {
    private val sekvensnummerProducer = kafkaConfig.nextSekvensnummerProducer()

    internal fun writeSekvensnummer(sekvensnummer: Long, synchronous: Boolean = false) {
        val record = ProducerRecord(NEXT_SEKVENSNUMMER_TOPIC, "sekvensnummer", sekvensnummer.toString())
        if (synchronous) {
            sekvensnummerProducer.send(record)
            persistedSekvensnummerGauge.set(record.value().toDouble())
        } else {
            sekvensnummerProducer.send(record, callBack(record))
        }
    }

    internal fun close() = sekvensnummerProducer.close()
}

private fun callBack(record: ProducerRecord<String, String>): (metadata: RecordMetadata?, exception: Exception?) -> Unit =
        { recordMetadata, exception ->
            if (exception == null) {
                LOG.info("""Added sekvensnummer "${record.value()}" to topic ${record.topic()}""")
                persistedSekvensnummerGauge.set(record.value().toDouble())
            } else {
                LOG.error("""Error while sending sekvensnummer "${record.value()}" """)
                throw exception
            }
        }

private val persistedSekvensnummerGauge = Gauge.build()
        .name("persistedSekvensnummer")
        .help("Siste persisterte som brukes når det hentes pgi-hendelser fra skatt").register()