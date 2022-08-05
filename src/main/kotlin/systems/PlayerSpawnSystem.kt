package systems

import entities.Player
import entities.SpawnPoint
import shared.Vec2
import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.input.Button
import no.njoh.pulseengine.core.scene.SceneState.RUNNING
import no.njoh.pulseengine.core.scene.SceneSystem
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.Extensions.forEachFast
import shared.TEMPLATE_NAME
import kotlin.math.sqrt

class PlayerSpawnSystem : SceneSystem()
{
    var templatePlayerId = -1L

    @JsonIgnore var activePlayers = mutableListOf<Long>()

    override fun onCreate(engine: PulseEngine)
    {
        // Players transferred from previous scene
        playersToTransfer.forEachFast()
        {
            val player = it.copy()
            val spawnCoordinate = findSpawnCoordinate(engine)
            player.x = spawnCoordinate.x
            player.y = spawnCoordinate.y
            player.onStart(engine)
            player.init(engine)
            player.setAlive()
            engine.scene.addEntity(player)
            activePlayers.add(player.id)
        }
        playersToTransfer.clear()
    }

    override fun onUpdate(engine: PulseEngine)
    {
        if (engine.scene.state != RUNNING)
            return

        // Add players when START is pressed
        for (gamepad in engine.input.gamepads)
        {
            if (gamepad.isPressed(Button.START))
            {
                if (activePlayers.none { id -> engine.scene.getEntityOfType<Player>(id)?.gamepadId == gamepad.id })
                    addPlayer(engine, gamepad.id)
            }
        }
    }

    private fun addPlayer(engine: PulseEngine, gamepadId: Int)
    {
        val templatePlayer = engine.scene.getEntityOfType<Player>(templatePlayerId) ?: return
        val newPlayer = templatePlayer.copy()
        val playerSpec = getRandomPlayerSpec(engine)
        val spawnCoordinate = findSpawnCoordinate(engine)

        newPlayer.gamepadId = gamepadId
        newPlayer.name = playerSpec.name
        newPlayer.color = playerSpec.color
        newPlayer.x = spawnCoordinate.x
        newPlayer.y = spawnCoordinate.y
        newPlayer.onStart(engine)
        newPlayer.init(engine)
        engine.scene.addEntity(newPlayer)
        activePlayers.add(newPlayer.id)
    }

    private fun findSpawnCoordinate(engine: PulseEngine): Vec2
    {
        var longestDistFromPlayers = 0f
        var longestDistSpawnPoint: SpawnPoint? = null
        val spawnPoints = engine.scene.getAllEntitiesOfType<SpawnPoint>() ?: emptyList()

        // Find the spawn point furthest away from other players
        for (spawnPoint in spawnPoints)
        {
            // Find the closest player to the spawn point
            var smallestDist = Float.MAX_VALUE
            engine.scene.getAllEntitiesOfType<Player>()?.forEachFast { player ->
                val xd = spawnPoint.x - player.x
                val yd = spawnPoint.y - player.y
                val dist = sqrt(xd * xd + yd * yd)
                if (dist < smallestDist)
                    smallestDist = dist
            }

            // Save the spawn point farthest away from the closest player
            if (smallestDist > longestDistFromPlayers)
            {
                longestDistFromPlayers = smallestDist
                longestDistSpawnPoint = spawnPoint
            }
        }

        return Vec2(longestDistSpawnPoint?.x ?: 0f, longestDistSpawnPoint?.y ?: 0f)
    }

    private fun getRandomPlayerSpec(engine: PulseEngine): PlayerSpec
    {
        val existingNames = mutableSetOf<String>()
        engine.scene.forEachEntityOfType<Player> { existingNames.add(it.name) }
        playerSpec.shuffled().forEachFast()
        {
            if (it.name !in existingNames)
                return it
        }
        return PlayerSpec(name = "P${engine.scene.getAllEntitiesOfType<Player>()?.size ?: 0}", Color(1f, 1f, 1f))
    }

    data class PlayerSpec(val name: String, val color: Color)

    companion object
    {
        @JsonIgnore
        private val playersToTransfer = mutableListOf<Player>()
        private val playerSpec = listOf(
            PlayerSpec("Knut-Tore",  Color(232, 97, 97)),
            PlayerSpec("Finn-Olav",  Color(97, 133, 232)),
            PlayerSpec("Kris-Ove",   Color(232, 97, 221)),
            PlayerSpec("Bent-Arne",  Color(209, 143, 63)),
            PlayerSpec("Stig-Jonny", Color(119, 209, 63)),
            PlayerSpec("Nils-Roger", Color(99, 67, 161))
        )

        fun transferPlayer(player: Player)
        {
            if (player.name != TEMPLATE_NAME) playersToTransfer.add(player)
        }
    }
}