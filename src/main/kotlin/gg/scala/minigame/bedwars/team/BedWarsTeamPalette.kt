package gg.scala.minigame.bedwars.team

import gg.tropic.practice.games.team.TeamIdentifier

/**
 * @author Limo
 * @date 2/13/2026
 */
object BedWarsTeamPalette
{
    fun woolData(teamIdentifier: TeamIdentifier) = when (teamIdentifier.label)
    {
        "A" -> 14
        "B" -> 11
        "C" -> 5
        "D" -> 4
        "E" -> 9
        "F" -> 0
        "G" -> 6
        "H" -> 7
        else -> 15
    }
}
