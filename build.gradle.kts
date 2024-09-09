import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorSupportVersion = "0.0.22"
val ktorVersion = "2.3.4"
val maskinportenClientVersion = "0.0.9"
val joseJwtVersion = "9.0.1"
val micrometerVersion = "1.3.5"
val slf4jVersion = "2.0.9"
val kafkaVersion = "3.5.1"
val junitJupiterVersion = "5.11.0"
val assertJVersion = "3.26.3"
val kafkaEmbeddedEnvVersion = "3.2.4"
val wiremockVersion = "3.9.1"
val javaxEl = "3.0.1-b06"

val pgiDomainVersion = "0.0.5"
val jacksonVersion = "2.17.2"
val kotlinxCoroutinesVersion = "1.8.1"
val jerseyVersion = "3.1.8"


group = "no.nav.pgi"

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    id("com.github.ben-manes.versions") version "0.50.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
    maven("https://maven.pkg.github.com/navikt/pensjon-samhandling-ktor-support") {
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    maven("https://maven.pkg.github.com/navikt/pgi-domain") {
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
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxCoroutinesVersion")

    implementation("no.nav.pensjonopptjening:pensjon-opptjening-gcp-maskinporten-client:$maskinportenClientVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:$joseJwtVersion")

    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("no.nav.pgi:pgi-domain:$pgiDomainVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("net.logstash.logback:logstash-logback-encoder:5.2")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.wiremock:wiremock:$wiremockVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")

    testImplementation(("org.glassfish.jersey.core:jersey-server:$jerseyVersion"))
    testImplementation(("org.glassfish.jersey.core:jersey-common:$jerseyVersion"))
    testImplementation(("org.glassfish.jersey.core:jersey-client:$jerseyVersion"))
    testImplementation(("org.glassfish.jersey.inject:jersey-hk2:$jerseyVersion"))
    testImplementation("jakarta.xml.bind:jakarta.xml.bind-api:3.0.1")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")

    testImplementation("no.nav:kafka-embedded-env:$kafkaEmbeddedEnvVersion") {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        testRuntimeOnly("javax.el:javax.el-api:$javaxEl")
        testRuntimeOnly("org.glassfish:javax.el:$javaxEl")
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
    gradleVersion = "8.10"
}