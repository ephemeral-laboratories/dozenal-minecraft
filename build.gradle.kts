
version = "1.0"
group = "garden.ephemeral.dozenal"

apply(plugin = "base")

tasks.register<Zip>("resourcePack") {
    archiveFileName.set("dozenal-resource-pack-${project.version}.zip")
    destinationDirectory.set(file("${buildDir}/dist"))
    from("resource-pack")
}
tasks.named("assemble") {
    dependsOn("resourcePack")
}