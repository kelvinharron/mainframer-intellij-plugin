import org.gradle.api.tasks.SourceSet
import java.util.regex.Pattern
import java.util.EnumSet
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.*
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

buildscript {
    repositories {
        jcenter()
        maven { setUrl("http://dl.bintray.com/jetbrains/intellij-plugin-service") }
    }
}

plugins {
    jacoco
    id("org.jetbrains.intellij") version "0.7.2"
    kotlin("jvm") version "1.3.31"
}
val kotlinVersion = plugins.getPlugin(KotlinPluginWrapper::class.java).kotlinPluginVersion

group = "com.elpassion.mainframerplugin"
version = readVersion()

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    sourceSets[SourceSet.MAIN_SOURCE_SET_NAME].java {
        srcDir("src/main/kotlin")
    }
}

tasks.withType<JacocoReport> {
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
    val check by tasks
    check.dependsOn(this)
}

repositories {
    jcenter()
}

intellij {
    version = "IC-2019.1"
    pluginName = "mainframer-integration"
    updateSinceUntilBuild = true
}

val publishPlugin: PublishTask by tasks
val patchPluginXml: PatchPluginXmlTask by tasks

publishPlugin {
    username(project.findProperty("MF_PUBLISH_USER_NAME") as String?)
    password(project.findProperty("MF_PUBLISH_PASSWORD") as String?)
    channels(listOf(project.findProperty("publishChannel") as String?))
}

patchPluginXml {
    sinceBuild("181.2784.17")
    untilBuild("203.*")
}

fun readVersion(): String {
    val versionFile = File("version.gradle.properties")
    val matcher = Pattern.compile("version=(\\d+.\\d+.\\d+)").matcher(versionFile.readText())
    if (matcher.find()) {
        return matcher.group(1)
    } else {
        throw RuntimeException("Version not found!")
    }
}

task("updateVersion") {
    doLast {
        val versionFile = File("version.gradle.properties")
        val matcher = Pattern.compile("version=(\\d+.\\d+.\\d+)").matcher(versionFile.readText())
        if (matcher.find()) {
            val versionText = StringBuilder(versionFile.readText()).replace(matcher.start(1), matcher.end(1), project.toString())
            versionFile.writeText(versionText.toString())
        }
    }
}
tasks.runPluginVerifier {
    val intellijUltimate20203Release = "IU-203.7717.56"
    ideVersions(intellijUltimate20203Release)
    failureLevel = EnumSet.of(COMPATIBILITY_PROBLEMS, INVALID_PLUGIN, MISSING_DEPENDENCIES)
}

dependencies {
    implementation(kotlin("stdlib", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.6")
    implementation("com.squareup.retrofit2:retrofit:2.1.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.1.0")
    implementation("io.reactivex.rxjava2:rxjava:2.0.5")
    implementation("com.jakewharton.retrofit:retrofit2-rxjava2-adapter:1.0.0")

    testImplementation("com.nhaarman:mockito-kotlin:1.2.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:3.6.0")
    testImplementation("junit:junit:4.11")
    testImplementation("org.assertj:assertj-core:3.6.1")
}

inline operator fun <T : Task> T.invoke(a: T.() -> Unit): T = apply(a)
