import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion

allprojects {
  group = "net.orbis"
  version = "0.1.0-SNAPSHOT"

  repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
  }
}

subprojects {
  plugins.withId("java") {
    extensions.configure<JavaPluginExtension>("java") {
      toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    tasks.withType<JavaCompile>().configureEach {
      options.release.set(21)
      options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
      useJUnitPlatform()
      testLogging {
        exceptionFormat = TestExceptionFormat.FULL
      }
    }
  }
}
