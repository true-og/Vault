// build.gradle.kts
/* This is free and unencumbered software released into the public domain */

import org.gradle.kotlin.dsl.provideDelegate

/* ------------------------------ Plugins ------------------------------ */
plugins {
    id("java") // Import Java plugin.
    id("java-library") // Import Java Library plugin.
    id("com.diffplug.spotless") version "7.0.4" // Import Spotless plugin.
    id("com.gradleup.shadow") version "8.3.6" // Import Shadow plugin.
    id("checkstyle") // Import Checkstyle plugin.
    eclipse // Import Eclipse plugin.
    kotlin("jvm") version "2.1.21" // Import Kotlin JVM plugin.
}

extra["kotlinAttribute"] = Attribute.of("kotlin-tag", Boolean::class.javaObjectType)

val kotlinAttribute: Attribute<Boolean> by rootProject.extra

/* --------------------------- JDK / Kotlin ---------------------------- */
java {
    sourceCompatibility = JavaVersion.VERSION_17 // Compile with JDK 17 compatibility.
    toolchain { // Select Java toolchain.
        languageVersion.set(JavaLanguageVersion.of(17)) // Use JDK 17.
        vendor.set(JvmVendorSpec.GRAAL_VM) // Use GraalVM CE.
    }
}

kotlin { jvmToolchain(17) }

/* ----------------------------- Metadata ------------------------------ */
group = "net.milkbowl.vault" // Declare bundle identifier.

version = "1.7.3" // Declare plugin version (will be in .jar).

val apiVersion = "1.19" // Declare minecraft server target version.

/* --------------------------- Source layout --------------------------- */
sourceSets {
    named("main") {
        java.setSrcDirs(listOf("src"))
        java.exclude("**/module-info.java")
    }
}

/* ----------------------------- Resources ----------------------------- */
tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to version, "apiVersion" to apiVersion)
    inputs.properties(props) // Indicates to rerun if version changes.
    filesMatching("plugin.yml") { expand(props) }
    from(".") {
        include("plugin.yml")
        expand(props)
    }
    from("LICENSE") { into("/") } // Bundle licenses into jarfiles.
}

/* ---------------------------- Repos ---------------------------------- */
repositories {
    mavenCentral() // Import the Maven Central Maven Repository.
    gradlePluginPortal() // Import the Gradle Plugin Portal Maven Repository.
    maven { url = uri("https://repo.purpurmc.org/snapshots") } // Import the PurpurMC Maven Repository.
    maven { url = uri("https://nexus.hc.to/content/repositories/pub_releases/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/groups/public/") }
    maven { url = uri("https://dev.escapecraft.com/maven") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public") }
    maven { url = uri("file://${System.getProperty("user.home")}/.m2/repository") }
    System.getProperty("SELF_MAVEN_LOCAL_REPO")?.let {
        val dir = file(it)
        if (dir.isDirectory) {
            println("Using SELF_MAVEN_LOCAL_REPO at: $it")
            maven { url = uri("file://${dir.absolutePath}") }
        } else {
            logger.error("TrueOG Bootstrap not found, defaulting to ~/.m2 for mavenLocal()")
            mavenLocal()
        }
    } ?: logger.error("TrueOG Bootstrap not found, defaulting to ~/.m2 for mavenLocal()")
}

/* ---------------------- Java project deps ---------------------------- */
dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT") // Declare Purpur API version to be packaged.
    implementation("net.milkbowl.vault:VaultAPI:1.7")
    implementation("org.bstats:bstats-bukkit:3.0.0")
    implementation("org.apache.clerezza.ext:org.json.simple:0.4")
    compileOnly(files("lib/DroxPerms.jar"))
    compileOnly(files("lib/Privileges.jar"))
    compileOnly(files("lib/Xperms.jar"))
    compileOnly(files("lib/EssentialsGroupManager.jar"))
    compileOnly(files("lib/iChat.jar"))
    compileOnly(files("lib/mChat.jar"))
    compileOnly(files("lib/mChatSuite.jar"))
    compileOnly(files("lib/Permissions.jar"))
    compileOnly(files("lib/PermissionsBukkit.jar"))
    compileOnly(files("lib/PermissionsEx.jar"))
    compileOnly(files("lib/bpermissions25.jar"))
    compileOnly(files("lib/Starburst.jar"))
    compileOnly(files("lib/SimplyPerms.jar"))
    compileOnly(files("lib/CommandsEX.jar"))
    compileOnly(files("lib/TotalPermissions.jar"))
    compileOnly(files("lib/KPerms.jar"))
    compileOnly(files("lib/overpermissions-2.0.0.jar"))
}

/* ---------------------- Reproducible jars ---------------------------- */
tasks.withType<AbstractArchiveTask>().configureEach { // Ensure reproducible .jars
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

/* ----------------------------- Shadow -------------------------------- */
tasks.shadowJar {
    exclude("io.github.miniplaceholders.*") // Exclude the MiniPlaceholders package from being shadowed.
    archiveClassifier.set("") // Use empty string instead of null.
    archiveBaseName.set("Vault")
    archiveFileName.set("Vault-${project.version}.jar")
    minimize()
    relocate("org.bstats", "net.milkbowl.vault.metrics")
    dependencies {
        include(dependency("net.milkbowl.vault:VaultAPI"))
        include(dependency("org.bstats:bstats-bukkit"))
    }
}

tasks.jar { archiveClassifier.set("part") } // Applies to root jarfile only.

tasks.build { dependsOn(tasks.spotlessApply, tasks.shadowJar) } // Build depends on spotless and shadow.

/* --------------------------- Javac opts ------------------------------- */
tasks.withType<JavaCompile>().configureEach {
    modularity.inferModulePath.set(false)
    options.compilerArgs.add("-parameters") // Enable reflection for java code.
    options.isFork = true // Run javac in its own process.
    options.compilerArgs.add("-Xlint:deprecation") // Trigger deprecation warning messages.
    options.encoding = "UTF-8" // Use UTF-8 file encoding.
}

/* ----------------------------- Auto Formatting ------------------------ */
spotless {
    java {
        eclipse().configFile("config/formatter/eclipse-java-formatter.xml") // Eclipse java formatting.
        leadingTabsToSpaces() // Convert leftover leading tabs to spaces.
        removeUnusedImports() // Remove imports that aren't being called.
    }
    kotlinGradle {
        ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) } // JetBrains Kotlin formatting.
        target("build.gradle.kts", "settings.gradle.kts") // Gradle files to format.
    }
}

checkstyle {
    toolVersion = "10.18.1" // Declare checkstyle version to use.
    configFile = file("config/checkstyle/checkstyle.xml") // Point checkstyle to config file.
    isIgnoreFailures = true // Don't fail the build if checkstyle does not pass.
    isShowViolations = true // Show the violations in any IDE with the checkstyle plugin.
}

tasks.named("compileJava") {
    dependsOn("spotlessApply") // Run spotless before compiling with the JDK.
}

tasks.named("spotlessCheck") {
    dependsOn("spotlessApply") // Run spotless before checking if spotless ran.
}
