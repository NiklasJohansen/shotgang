package systems

import com.fasterxml.jackson.annotation.JsonIgnore
import entities.Player
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Font
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.core.scene.SceneSystem
import no.njoh.pulseengine.core.shared.primitives.Color
import util.playSoundWithName
import util.setDrawColor
import kotlin.math.max

class GameStateSystem : SceneSystem()
{
    var fontName = ""
    var fontSize = 200f
    var fontColor = Color(1f, 0.67f, 0f)
    var nextLevelTime = 2000f
    var gameOverFadeTime = 2000f
    var gameCountDownTime = 3000f

    @JsonIgnore var gameStarted = false
    @JsonIgnore var gameOver = false
    @JsonIgnore var scoreUpdated = false
    @JsonIgnore private var gameOverTime = 0L
    @JsonIgnore private var gameStartedTime = 0L
    @JsonIgnore private var transitionedToNextLevel = false
    @JsonIgnore private var lastCountDownSecond: Long = -1L

    override fun onStart(engine: PulseEngine)
    {
        gameStartedTime = System.currentTimeMillis()
    }

    override fun onUpdate(engine: PulseEngine)
    {
        if (engine.scene.state != SceneState.RUNNING)
            return

        if (!gameStarted)
            prepareGameStart(engine)

        if (gameOver)
            finishGame(engine)
        else
            checkIfGameIsOver(engine)
    }

    private fun prepareGameStart(engine: PulseEngine)
    {
        val elapsedGameStartTime = System.currentTimeMillis() - gameStartedTime
        val second = (elapsedGameStartTime / 1000L)
        if (!gameStarted && second != lastCountDownSecond)
        {
            // Play count down ding every second
            val soundName = if (elapsedGameStartTime > gameCountDownTime) "countdown_ding_1" else "countdown_ding_0"
            engine.playSoundWithName(soundName)
            lastCountDownSecond = second
        }

        if (elapsedGameStartTime > gameCountDownTime)
            gameStarted = true
    }

    private fun checkIfGameIsOver(engine: PulseEngine)
    {
        var totalPlayers = 0
        var livingPlayers = 0
        engine.scene.forEachEntityOfType<Player>()
        {
            totalPlayers++
            if (!it.isDead())
                livingPlayers++
        }

        if ((totalPlayers > 1 && livingPlayers < 2) || (totalPlayers == 1 && livingPlayers == 0))
        {
            gameOver = true
            gameOverTime = System.currentTimeMillis()
        }
    }

    private fun finishGame(engine: PulseEngine)
    {
        val elapsedGameOverTime = System.currentTimeMillis() - gameOverTime
        if (!scoreUpdated && elapsedGameOverTime > 500) // Wait 500ms in case last living player is about to die and the result is a DRAW
        {
            getLivingPlayer(engine)?.let { it.wins++ }
            scoreUpdated = true
        }

        if (!transitionedToNextLevel && !isLastLevel() && elapsedGameOverTime > gameOverFadeTime + nextLevelTime)
        {
            transitionToNextLevel(engine)
            transitionedToNextLevel = true
        }
    }

    override fun onRender(engine: PulseEngine)
    {
        val surface = engine.gfx.getSurface("overlay") ?: return
        val elapsedStartTime = System.currentTimeMillis() - gameStartedTime

        // Count down text
        if (elapsedStartTime < gameCountDownTime + 1000)
        {
            surface.setDrawColor(fontColor)
            surface.drawText(
                text = if (elapsedStartTime > gameCountDownTime) "GO" else "${max(0f, (gameCountDownTime / 1000) - (elapsedStartTime / 1000)).toInt()}",
                x = engine.window.width.toFloat() / 2f,
                y = engine.window.height.toFloat() / 2f,
                font = engine.asset.getOrNull(fontName),
                fontSize = fontSize, xOrigin = 0.5f, yOrigin = 0.5f)
        }

        if (gameOver)
        {
            // Full screen background
            val elapsedTime = System.currentTimeMillis() - gameOverTime
            val alpha = (elapsedTime.toFloat() / gameOverFadeTime).coerceIn(0f, 1f)
            surface.setDrawColor(0f, 0f, 0f, alpha)
            surface.drawTexture(Texture.BLANK, 0f, 0f, engine.window.width.toFloat(), engine.window.height.toFloat())

            // Winner text
            val sortedPlayers = engine.scene.getAllEntitiesOfType<Player>()?.sortedByDescending { it.wins * 100 + it.kills } ?: return
            val winningPlayer = getWinningPlayer(sortedPlayers)
            val showFinalWinner = isLastLevel() && winningPlayer != null && !transitionedToNextLevel
            val color = if (showFinalWinner) GOLD else winningPlayer?.color ?: WHITE
            val text = if (winningPlayer == null) "DRAW" else "${winningPlayer.name} WINS!"
            val font = engine.asset.getOrNull<Font>("badabb")
            surface.setDrawColor(color, alpha)
            surface.drawText(text, engine.window.width / 2f, engine.window.height / 3f, font, fontSize = 100f, xOrigin = 0.5f, yOrigin = 0.5f)

            // entities.Player stat cards
            for ((i, player) in sortedPlayers.withIndex())
            {
                val step = engine.window.width / sortedPlayers.size.toFloat()
                val x = 0.5f * (engine.window.width) - 0.5f * (engine.window.width - step) + step * i
                val y = engine.window.height * 0.5f

                if (showFinalWinner && i == 0)
                {
                    // Gold border on winning player
                    surface.setDrawColor(GOLD, alpha)
                    surface.drawTexture(Texture.BLANK, x, y - 10f,  250f + 20, engine.window.height / 4f + 20, xOrigin = 0.5f, cornerRadius = 20f)
                }

                // Card background rect
                surface.setDrawColor(DARK, alpha)
                surface.drawTexture(Texture.BLANK, x, y,  250f, engine.window.height / 4f, xOrigin = 0.5f, cornerRadius = 20f)

                // entities.Player name
                surface.setDrawColor(player.color, alpha)
                surface.drawText(player.name, x, y + 50, font, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 50f)

                // entities.Player stats
                surface.setDrawColor(WHITE, alpha)
                surface.drawText("WINS: ${player.wins}", x, y + 120, font, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 40f)
                surface.drawText("KILLS: ${player.kills}", x, y + 180, font, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 40f)
            }
        }
    }

    private fun getLivingPlayer(engine: PulseEngine): Player? =
        engine.scene.getAllEntitiesOfType<Player>()?.firstOrNull { !it.isDead() }

    private fun getWinningPlayer(players: List<Player>): Player? =
        if (isLastLevel() && !transitionedToNextLevel && (players.size == 1 || !players[0].hasSameScore(players[1])))
            players[0]
        else
            players.firstOrNull { !it.isDead() }

    private fun isLastLevel() =
        currentLevelIndex >= queuedLevelNames.lastIndex

    private fun Player.hasSameScore(otherPlayer: Player) =
        wins == otherPlayer.wins && kills == otherPlayer.kills

    companion object
    {
        private val WHITE = Color(1f, 1f, 1f)
        private val GOLD = Color(1f, 0.9f, 0f)
        private val DARK = Color(0.1f, 0.1f, 0.1f)
        private val levelNames = listOf(
            "levels/shotgang_level_concrete_0.scn",
            "levels/shotgang_level_concrete_1.scn",
            "levels/shotgang_level_concrete_2.scn",
            "levels/shotgang_level_grass_0.scn",
            "levels/shotgang_level_grass_1.scn",
            "levels/shotgang_level_grass_2.scn",
            "levels/shotgang_level_desert_0.scn",
            "levels/shotgang_level_desert_1.scn",
            "levels/shotgang_level_desert_2.scn"
        )
        private var currentLevelIndex = -1
        private val REPEAT_LEVEL_COUNT = 3
        private val queuedLevelNames =
            (0 until REPEAT_LEVEL_COUNT).flatMap { levelNames.shuffled() }

        fun transitionToNextLevel(engine: PulseEngine)
        {
            if (currentLevelIndex < queuedLevelNames.lastIndex)
            {
                engine.scene.forEachEntityOfType<Player> { PlayerSpawnSystem.transitionPlayer(it) }
                engine.scene.transitionInto(fileName = queuedLevelNames[++currentLevelIndex], fromClassPath = true, fadeTimeMs = 500)
            }
        }
    }
}