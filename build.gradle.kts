import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorSupportVersion = "0.0.22"
val ktorVersion = "2.3.4"
val maskinportenClientVersion = "0.0.9"
val joseJwtVersion = "9.0.1"
val micrometerVersion = "1.3.5"
val slf4jVersion = "2.0.9"
val kafkaVersion = "3.5.1"
val kafkaAvroSerializerVersion = "7.1.0"
val pgiSchemaVersion = "0.0.7"
val junitJupiterVersion = "5.6.0"
val kafkaEmbeddedEnvVersion = "3.1.0"
val wiremockVersion = "2.27.2"

group = "no.nav.pgi"

plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    jcenter()
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
    maven("https://maven.pkg.github.com/navikt/pensjon-samhandling-ktor-support") {
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    maven("https://maven.pkg.github.com/navikt/pgi-schema") {
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    maven("https://maven.pkg.github.com/navikt/pensjon-opptjening-gcp-maskinporten-client") {
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("no.nav.pensjonsamhandling:pensjon-samhandling-ktor-support:$ktorSupportVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")

    implementation("no.nav.pensjonopptjening:pensjon-opptjening-gcp-maskinporten-client:$maskinportenClientVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:$joseJwtVersion")

    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("io.confluent:kafka-avro-serializer:$kafkaAvroSerializerVersion")
    implementation("no.nav.pgi:pgi-schema:$pgiSchemaVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("net.logstash.logback:logstash-logback-encoder:5.2")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("com.github.tomakehurst:wiremock:$wiremockVersion")
    testImplementation("no.nav:kafka-embedded-env:$kafkaEmbeddedEnvVersion") {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
}

configurations {
    all {
        exclude(group = "log4j", module = "log4j")
    }
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("app")

    manifest {
        attributes["Main-Class"] = "no.nav.pgi.skatt.leshendelse.ApplicationKt"
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
            it.name
        }
    }

    doLast {
        configurations.runtimeClasspath.get().forEach {
            val file = File("$buildDir/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = FULL
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "7.6"
}

