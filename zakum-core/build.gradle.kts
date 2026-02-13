import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  `java-library`
  alias(libs.plugins.shadow)
}

dependencies {
  api(project(":zakum-api"))

  compileOnly(libs.paper.api)

  implementation(libs.hikaricp)
  implementation(libs.flyway.core)
  implementation(libs.mysql)
  implementation(libs.caffeine)

  implementation(libs.slf4j.api)
  implementation(libs.slf4j.jdk14)

  compileOnly(libs.annotations)
}

tasks.processResources {
  filesMatching("plugin.yml") {
    expand("version" to project.version)
  }
}

tasks.jar {
  enabled = false
}

tasks.named<ShadowJar>("shadowJar") {
  archiveBaseName.set("Zakum")
  archiveClassifier.set("")

  // Keep MySQL driver canonical (no relocation).
  relocate("com.github.benmanes.caffeine", "net.orbis.zakum.libs.caffeine")
  relocate("com.zaxxer.hikari", "net.orbis.zakum.libs.hikari")
  relocate("org.flywaydb", "net.orbis.zakum.libs.flyway")
  relocate("org.slf4j", "net.orbis.zakum.libs.slf4j")

  isReproducibleFileOrder = true
  isPreserveFileTimestamps = false
}

tasks.build {
  dependsOn(tasks.named("shadowJar"))
}
