repositories {
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly(project(":game"))
    compileOnly(project(":shared"))
    compileOnly(project(":spigot-integration"))
    api(project(":minigames:bedwars-shared"))
    compileOnly("com.github.cryptomorin:XSeries:13.0.0")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
}
