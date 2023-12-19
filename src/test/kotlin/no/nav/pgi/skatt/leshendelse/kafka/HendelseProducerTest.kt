package no.nav.pgi.skatt.leshendelse.kafka

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
    private val kafkaFactory =
        KafkaHendelseFactory(KafkaConfig(kafkaTestEnvironment.kafkaTestEnvironmentVariables(), PlaintextStrategy()))
    private val hendelseProducer = HendelseProducer(kafkaFactory)

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

        assertEquals(hendelse.sekvensnummer, record.value().getSekvensnummer())
        assertEquals(hendelse.identifikator, record.value().getIdentifikator())
        assertEquals(hendelse.gjelderPeriode, record.value().getGjelderPeriode())
    }

    @Test
    fun `write three pgi hendelser to topic`() {
        val hendelse1 = HendelseDto("11111111111", "1111", 1L)
        val hendelse2 = HendelseDto("22222222222", "2222", 2L)
        val hendelse3 = HendelseDto("33333333333", "3333", 3L)

        hendelseProducer.writeHendelser(listOf(hendelse1, hendelse2, hendelse3))

        val record = kafkaTestEnvironment.consumeHendelseTopic()

        assertEquals(hendelse1.sekvensnummer, record[0].value().getSekvensnummer())
        assertEquals(hendelse1.identifikator, record[0].value().getIdentifikator())
        assertEquals(hendelse1.gjelderPeriode, record[0].value().getGjelderPeriode())

        assertEquals(hendelse2.sekvensnummer, record[1].value().getSekvensnummer())
        assertEquals(hendelse2.identifikator, record[1].value().getIdentifikator())
        assertEquals(hendelse2.gjelderPeriode, record[1].value().getGjelderPeriode())

        assertEquals(hendelse3.sekvensnummer, record[2].value().getSekvensnummer())
        assertEquals(hendelse3.identifikator, record[2].value().getIdentifikator())
        assertEquals(hendelse3.gjelderPeriode, record[2].value().getGjelderPeriode())
    }
}