plugins {
    java
    `maven-publish`
    id ("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "top.mrxiaom.sweet.afdian"
version = "1.0.5"
val targetJavaVersion = 8
val shadowGroup = "top.mrxiaom.sweet.afdian.libs"

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
    maven("https://repo.rosewooddev.io/repository/public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    // compileOnly("org.spigotmc:spigot:1.20") // NMS

    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("org.jetbrains:annotations:24.0.0")

    implementation("com.zaxxer:HikariCP:4.0.3") { isTransitive = false }
    implementation("com.github.technicallycoded:FoliaLib:0.4.4") { isTransitive = false }
    implementation("top.mrxiaom:PluginBase:1.5.8")
}
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}
tasks {
    shadowJar {
        mapOf(
            "com.tcoded.folialib" to "folialib",
            "top.mrxiaom.pluginbase" to "base",
            "com.zaxxer.hikari" to "hikari",
        ).forEach { (original, target) ->
            relocate(original, "$shadowGroup.$target")
        }
        listOf(
            "top/mrxiaom/pluginbase/func/AbstractGui*",
            "top/mrxiaom/pluginbase/func/gui/*",
            "top/mrxiaom/pluginbase/utils/IA*",
            "top/mrxiaom/pluginbase/utils/ItemStackUtil*",
            "top/mrxiaom/pluginbase/func/GuiManager*",
            "top/mrxiaom/pluginbase/gui/*",
            "top/mrxiaom/pluginbase/func/LanguageManager*",
            "top/mrxiaom/pluginbase/func/language/*",
            "top/mrxiaom/pluginbase/utils/Adventure*",
            "top/mrxiaom/pluginbase/utils/Bytes*",
        ).forEach(this::exclude)
    }
    val copyTask = create<Copy>("copyBuildArtifact") {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs)
        rename { "${project.name}-$version.jar" }
        into(rootProject.file("out"))
    }
    build {
        dependsOn(copyTask)
    }
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(sourceSets.main.get().resources.srcDirs) {
            expand(mapOf("version" to version))
            include("plugin.yml")
        }
    }
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components.getByName("java"))
            groupId = project.group.toString()
            artifactId = rootProject.name
            version = project.version.toString()
        }
    }
}
