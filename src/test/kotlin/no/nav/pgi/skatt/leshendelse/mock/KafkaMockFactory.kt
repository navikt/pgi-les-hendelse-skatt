package no.nav.pgi.skatt.leshendelse.mock

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.pgi.skatt.leshendelse.kafka.KafkaFactory
import no.nav.pgi.skatt.leshendelse.kafka.NEXT_SEKVENSNUMMER_TOPIC
import no.nav.pgi.skatt.leshendelse.kafka.SekvensnummerConsumer
import no.nav.samordning.pgi.schema.Hendelse
import no.nav.samordning.pgi.schema.HendelseKey
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.concurrent.Future


internal class KafkaMockFactory(
    internal val hendelseProducer: MockProducer<HendelseKey, Hendelse> = defaultHendelseProducer(),
    internal val nextSekvensnummerProducer: MockProducer<String, String> = defaultNextSekvensnummerProducer(),
    internal val nextSekvensnummerConsumer: MockConsumer<String, String> = defaultNextSekvensnummerConsumer(),
) : KafkaFactory {

    override fun hendelseProducer(): Producer<HendelseKey, Hendelse> = hendelseProducer
    override fun nextSekvensnummerProducer(): Producer<String, String> = nextSekvensnummerProducer
    override fun nextSekvensnummerConsumer(): Consumer<String, String> = nextSekvensnummerConsumer

    internal fun close() {
        hendelseProducer.apply { if (!closed()) close() }
        nextSekvensnummerProducer.apply { if (!closed()) close() }
        nextSekvensnummerConsumer.apply { if (!closed()) close() }
    }

    companion object {
        internal fun defaultHendelseProducer() =
            MockProducer(true, TypedAvroSerializer<HendelseKey>(), TypedAvroSerializer<Hendelse>())

        internal fun defaultNextSekvensnummerProducer() = MockProducer(true, StringSerializer(), StringSerializer())
        internal fun defaultNextSekvensnummerConsumer(): MockConsumer<String, String> =
            MockConsumer<String, String>(OffsetResetStrategy.LATEST).apply {
                assign(listOf(TopicPartition(NEXT_SEKVENSNUMMER_TOPIC, 0)))
                addRecord(ConsumerRecord(NEXT_SEKVENSNUMMER_TOPIC, 0, 1L, null, "1"))
                updateEndOffsets(mapOf(SekvensnummerConsumer.defaultTopicPartition to 2))
            }
    }
}

internal class ExceptionKafkaProducer<K : Any, V : Any>(
    keySerializer: TypedAvroSerializer<K> = TypedAvroSerializer(),
    valueSerializer: TypedAvroSerializer<V> = TypedAvroSerializer(),
) : MockProducer<K, V>(false, keySerializer, valueSerializer) {
    override fun send(record: ProducerRecord<K, V>?): Future<RecordMetadata> =
        super.send(record).also { this.errorNext(RuntimeException("Test exception")) }
}

internal class TypedAvroSerializer<T> : Serializer<T> {
    override fun serialize(topic: String?, data: T): ByteArray {
        return KafkaAvroSerializer(
            MockSchemaRegistry.getClientForScope("test"),
            mapOf(
                "auto.register.schemas" to true,
                "schema.registry.url" to "mock://test"
            )
        ).serialize(topic, data)
    }
}