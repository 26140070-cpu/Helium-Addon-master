plugins {
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName = "Helium-Addon"
    version = libs.versions.mod.version.get()
    group = properties["maven_group"] as String
}

repositories {
    mavenCentral()
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }

    flatDir {
        dirs("libs")
    }
}

dependencies {
    // Fabric
    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn) { classifier("v2") })
    modImplementation(libs.fabric.loader)

    // Meteor Client API
    modImplementation(libs.meteor.client)

    implementation("local.mod:yunglightui:1.0.0")
    include("local.mod:yunglightui:1.0.0")

    implementation("local.mod:catppuccin:1.0.0")
    include("local.mod:catppuccin:1.0.0")

}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to libs.versions.minecraft.get(),
            "name" to "Helium Addon"
        )

        inputs.properties(propertyMap)
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(propertyMap)

        }
    }

    jar {
        archiveFileName = "${base.archivesName.get()}-${project.version}.jar"

        from("LICENSE") {
            rename { "${it}_${base.archivesName.get()}" }
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }
}