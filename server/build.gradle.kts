plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
}

repositories {
    mavenCentral()
    google()
}

group = "org.menagerie.puppet_master"
version = "1.0.0"
application {
    mainClass.set("org.menagerie.puppet_master.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.contentnegotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.gson)
    implementation(libs.ktor.client.cio)
    implementation("org.apache.commons:commons-math3:3.6.1")
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
