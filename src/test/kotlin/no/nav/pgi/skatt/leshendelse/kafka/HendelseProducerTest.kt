package no.nav.pgi.skatt.leshendelse.kafka

import no.nav.pgi.domain.Hendelse
import no.nav.pgi.domain.serialization.PgiDomainSerializer
import no.nav.pgi.skatt.leshendelse.common.KafkaTestEnvironment
import no.nav.pgi.skatt.leshendelse.common.PlaintextStrategy
import no.nav.pgi.skatt.leshendelse.skatt.HendelseDto
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HendelseProducerTest {
    private val kafkaTestEnvironment = KafkaTestEnvironment()
    private val kafkaFactory = KafkaFactoryImpl(
        KafkaConfig(
            kafkaTestEnvironment.kafkaTestEnvironmentVariables(),
            PlaintextStrategy()
        )
    )
    private val hendelseProducer = HendelseProducer(kafkaFactory.hendelseProducer())

    @AfterAll
    internal fun teardown() {
        kafkaTestEnvironment.tearDown()
        hendelseProducer.close()
    }

    @Test
    fun `write one pgi hendelse to topic`() {
        val hendelse = HendelseDto("123456", "12345", 1L)
        hendelseProducer.writeHendelser(listOf(hendelse))

        val record = kafkaTestEnvironment.getFirstRecordOnTopic()

        val value = PgiDomainSerializer().fromJson(Hendelse::class, record.value())
        assertEquals(hendelse.sekvensnummer, value.sekvensnummer)
        assertEquals(hendelse.identifikator, value.identifikator)
        assertEquals(hendelse.gjelderPeriode, value.gjelderPeriode)
    }

    @Test
    fun `write three pgi hendelser to topic`() {
        val hendelse1 = HendelseDto("11111111111", "1111", 1L)
        val hendelse2 = HendelseDto("22222222222", "2222", 2L)
        val hendelse3 = HendelseDto("33333333333", "3333", 3L)

        hendelseProducer.writeHendelser(listOf(hendelse1, hendelse2, hendelse3))

        val record = kafkaTestEnvironment.consumeHendelseTopic()

        val value0 = PgiDomainSerializer().fromJson(Hendelse::class, record[0].value())
        val value1 = PgiDomainSerializer().fromJson(Hendelse::class, record[1].value())
        val value2 = PgiDomainSerializer().fromJson(Hendelse::class, record[2].value())

        assertEquals(hendelse1.sekvensnummer, value0.sekvensnummer)
        assertEquals(hendelse1.identifikator, value0.identifikator)
        assertEquals(hendelse1.gjelderPeriode, value0.gjelderPeriode)

        assertEquals(hendelse2.sekvensnummer, value1.sekvensnummer)
        assertEquals(hendelse2.identifikator, value1.identifikator)
        assertEquals(hendelse2.gjelderPeriode, value1.gjelderPeriode)

        assertEquals(hendelse3.sekvensnummer, value2.sekvensnummer)
        assertEquals(hendelse3.identifikator, value2.identifikator)
        assertEquals(hendelse3.gjelderPeriode, value2.gjelderPeriode)
    }
}