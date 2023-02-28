import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("io.gitlab.arturbosch.detekt") apply false
    id("io.spring.dependency-management")
    id("org.springframework.boot") apply false
    id("com.google.cloud.artifactregistry.gradle-plugin") apply false
    kotlin("jvm") apply false
    kotlin("plugin.spring") apply false
    idea
    java
    `maven-publish`
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "com.google.cloud.artifactregistry.gradle-plugin")

    group = "telus.capsa"

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<Detekt> {
        jvmTarget = java.targetCompatibility.toString()
        config.setFrom(rootProject.file("detekt.yml"))
        allRules = false
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = java.targetCompatibility.toString()
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    repositories {
        maven { setUrl("https://repo1.maven.org/maven2") }
        mavenCentral()
        google()
        maven { setUrl("https://mvnrepository.com/artifact") }
        maven { setUrl("https://dl.bintray.com/kotlin/kotlinx.html") }
    }

    dependencyManagement {
        imports {
            mavenBom("io.projectreactor:reactor-bom:${CoreVersion.REACTOR_BOM}")
        }
        dependencies {
            dependency("com.willowtreeapps.assertk:assertk-jvm:${CoreVersion.ASSERTK_JVM}")
            dependency("com.github.tomakehurst:wiremock-jre8:${CoreVersion.WIREMOCK_JRE8}")
            dependency("com.google.guava:guava:${CoreVersion.GUAVA}")
            dependency("org.springdoc:springdoc-openapi-ui:${CoreVersion.OPENAPI}")
            dependency("org.springframework.integration:spring-integration-mail:${CoreVersion.SPRING_MAIL}")
            dependency("org.apache.jmeter:ApacheJMeter_http:${CoreVersion.JMETER}") {
                exclude("org.apache.logging.log4j:log4j-slf4j-impl")
                exclude("org.apache.jmeter:bom")
            }
            dependency("kg.apc:jmeter-plugins-casutg:${CoreVersion.JMETER_JPGC_CASUTG}") {
                exclude("org.apache.logging.log4j:log4j-slf4j-impl")
            }
        }
    }

    tasks.named<Test>("test") {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
            excludeEngines("junit-vintage")
        }
        //testLogging.showStandardStreams = false
        testLogging {
            showCauses = true
            showExceptions = true
            showStackTraces = true
            showStandardStreams = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            afterSuite(KotlinClosure2<TestDescriptor, TestResult, Any>({ desc, result ->
                if (desc.parent == null) { // will match the outermost suite
                    val output =
                        "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
                    println(output)
                }
            }))
        }
    }

    tasks.getByName<BootJar>("bootJar") {
        enabled = false
    }

    tasks.getByName<Jar>("jar") {
        enabled = true
    }

    publishing {
        publications {
            create<MavenPublication>("default") {
                from(components["java"])
            }
        }

        repositories {
            maven {
                name = "Google Artifact Registry"
                url = uri("artifactregistry://northamerica-northeast1-maven.pkg.dev/cio-gke-devops-e4993356/telus-maven")
            }
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/telus/capsa-telus-libs")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}