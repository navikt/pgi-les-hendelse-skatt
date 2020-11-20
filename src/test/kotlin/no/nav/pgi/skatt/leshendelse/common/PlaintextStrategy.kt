package no.nav.pgi.skatt.leshendelse.common

import no.nav.pgi.skatt.leshendelse.kafka.KafkaConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol

internal class PlaintextStrategy : KafkaConfig.SecurityStrategy {
    override fun securityConfig() = mapOf(
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.PLAINTEXT.name
    )
}