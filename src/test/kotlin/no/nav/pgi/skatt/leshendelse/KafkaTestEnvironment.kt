package no.nav.pgi.skatt.leshendelse

import no.nav.common.KafkaEnvironment
import no.nav.pgi.skatt.leshendelse.KafkaConfig.Companion.NEXT_SEKVENSNUMMER_TOPIC

class KafkaTestEnvironment {
    private val kafkaTestEnvironment: KafkaEnvironment = KafkaEnvironment(topicNames = listOf(NEXT_SEKVENSNUMMER_TOPIC))

    init {
        kafkaTestEnvironment.start()
    }

    internal fun tearDown() = kafkaTestEnvironment.tearDown()
}