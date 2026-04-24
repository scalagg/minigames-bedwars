package gg.scala.minigame.bedwars

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.core.plugin.Plugin
import gg.scala.commons.core.plugin.PluginAuthor
import gg.scala.commons.core.plugin.PluginAuthorComposite
import gg.scala.commons.core.plugin.PluginDependency
import gg.scala.commons.core.plugin.PluginDependencyComposite
import gg.scala.commons.core.plugin.PluginWebsite

/**
 * @author Limo
 * @date 2/13/2026
 */
@Plugin(
    name = "BedWars",
    version = "%remote%/%branch%/%id%"
)
@PluginAuthorComposite(
    PluginAuthor("Limo")
)
@PluginWebsite("https://scala.gg")
@PluginDependencyComposite(
    PluginDependency("scala-commons"),
    PluginDependency("Lemon"),
    PluginDependency("Minigames")
)
class MiniGameBedWars : ExtendedScalaPlugin()
