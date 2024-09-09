package no.nav.pgi.skatt.leshendelse.kafka

import no.nav.pgi.skatt.leshendelse.common.KafkaTestEnvironment
import no.nav.pgi.skatt.leshendelse.common.PlaintextStrategy
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class SekvensnummerProducerConsumerTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaFactory = KafkaHendelseFactory(
        KafkaConfig(
            kafkaTestEnvironment.kafkaTestEnvironmentVariables(),
            PlaintextStrategy()
        )
    )
    private val sekvensnummerProducer = SekvensnummerProducer(
        sekvensnummerProducer = kafkaFactory.nextSekvensnummerProducer()
    )
    private val sekvensnummerConsumer = SekvensnummerConsumer(
        consumer = kafkaFactory.nextSekvensnummerConsumer(),
        topicPartition = TopicPartition(NEXT_SEKVENSNUMMER_TOPIC, 0)
    )

    @AfterAll
    internal fun teardown() {
        kafkaTestEnvironment.tearDown()
        sekvensnummerProducer.close()
        sekvensnummerConsumer.close()
    }

    @Order(1)
    @Test
    fun `should return null when there is no sekvensnummer on topic given`() {
        assertEquals(null, sekvensnummerConsumer.getNextSekvensnummer())
    }

    @Test
    fun `should return last sekvensnummer on topic`() {
        val lastSekvensnummer = "5"
        addListOfSekvensnummerToTopic(listOf("1", "2", "3", "4", lastSekvensnummer))

        assertEquals(lastSekvensnummer, sekvensnummerConsumer.getNextSekvensnummer())
        assertEquals(lastSekvensnummer, sekvensnummerConsumer.getNextSekvensnummer())
    }

    @Test
    fun `write sekvensnummer to topic`() {
        sekvensnummerProducer.writeSekvensnummer(1234L)
        assertEquals("1234", sekvensnummerConsumer.getNextSekvensnummer())
    }

    private fun addListOfSekvensnummerToTopic(sekvensnummerList: List<String>) {
        sekvensnummerList.indices.forEach { i -> sekvensnummerProducer.writeSekvensnummer(sekvensnummerList[i].toLong()) }
    }
}

//TODO Vurder å fjerne denne testen