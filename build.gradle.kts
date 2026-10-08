plugins { java }

group = "ru.mamont.cpvisualizer"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.playpro.com/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")
    // Prefer the owner's server JAR; public checkouts build against the official Maven artifact.
    val coreProtectJar = file("libs/CoreProtect-24.1.jar")
    if (coreProtectJar.exists() && !providers.gradleProperty("coreProtectFromMaven").isPresent) {
        compileOnly(files(coreProtectJar))
        testImplementation(files(coreProtectJar))
    } else {
        compileOnly("net.coreprotect:coreprotect:24.1") { isTransitive = false }
        testImplementation("net.coreprotect:coreprotect:24.1") { isTransitive = false }
    }
    testImplementation("io.papermc.paper:paper-api:26.2.build.129-stable")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
val pluginVersion = project.version.toString()
tasks.processResources {
    inputs.property("pluginVersion", pluginVersion)
    filesMatching("plugin.yml") { expand("version" to pluginVersion) }
}
tasks.test { useJUnitPlatform() }
tasks.jar { archiveFileName.set("CPVisualizer.jar") }
