
version = "1.0"
group = "garden.ephemeral.dozenal"

var minecraftVersion = "1.15.2"
var forgeVersion = "31.2.22-1.15.x-i18n-fixes"
var shortForgeVersion = "31.2-i18n-fixes"

apply(plugin = "base")

tasks.register<Zip>("resourcePack") {
    archiveFileName.set("dozenal-resource-pack-${project.version}.zip")
    destinationDirectory.set(file("${buildDir}/dist"))
    from("resource-pack")
}

tasks.register<Zip>("modpack") {
    archiveFileName.set("dozenal-modpack-${project.version}.zip")
    destinationDirectory.set(file("${buildDir}/dist"))
    includeEmptyDirs = true

    into("/") {
        from("modpack/resources")
        rename("dot-minecraft/(.*$)", ".minecraft/$1")
        exclude("dot-minecraft/")
    }

    into(".minecraft") {
        from("modpack/resources/dot-minecraft")
    }

    into(".minecraft/resourcepacks/dozenal-${version}") {
        from("resource-pack")
    }

    into("/") {
        from("modpack/templates")
        filter { line: String -> line
            .replace("@MINECRAFT_VERSION@", minecraftVersion)
            .replace("@FORGE_VERSION@", forgeVersion)
            .replace("@SHORT_FORGE_VERSION@", shortForgeVersion)
        }
    }
}

tasks.named("assemble") {
    dependsOn("resourcePack", "modpack")
}