import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm") version "1.2.51"
    kotlin("kapt") version "1.2.51"
    antlr
}

dependencies {

    implementation(kotlin("stdlib-jdk8"))
    implementation("com.squareup:kotlinpoet:1.0.0-RC1")
    implementation("com.apollographql.apollo:apollo-api:1.0.0-alpha")

    implementation("com.squareup.moshi:moshi:1.6.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.6.0")

    antlr("org.antlr:antlr4:4.7.1")
    implementation("org.antlr:antlr4-runtime:4.7.1")

    testImplementation("junit:junit:4.12")
    testImplementation("com.google.truth:truth:0.30")
    testImplementation("com.google.testing.compile:compile-testing:0.11")
}

repositories {
    jcenter()
}

tasks.withType<AntlrTask> {
    arguments.addAll(listOf("-package", "com.apollographql.apollo.compiler.parsing"))
}

tasks.withType<KotlinCompile> {
    dependsOn("generateGrammarSource")
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "4.9"
}