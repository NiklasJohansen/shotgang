package systems

import com.fasterxml.jackson.annotation.JsonIgnore
import entities.Player
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Font
import no.njoh.pulseengine.core.asset.types.Texture.Companion.BLANK
import no.njoh.pulseengine.core.scene.SceneState.RUNNING
import no.njoh.pulseengine.core.scene.SceneSystem
import no.njoh.pulseengine.core.shared.primitives.Color
import shared.*
import kotlin.math.max

class GameStateSystem : SceneSystem()
{
    // State change timing props
    var gameCountDownTime = 3000f
    var gameOverFadeTime = 2000f
    var nextLevelTime = 2000f

    // Count down text styling
    var fontName = FONT_BADABB
    var fontSize = 200f
    var fontColor = Color(1f, 0.67f, 0f)

    // Game state
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
        if (engine.scene.state != RUNNING)
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
            val soundName = if (elapsedGameStartTime > gameCountDownTime) SOUND_DING_1 else SOUND_DING_0
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
            val livingPlayer = getLivingPlayer(engine)

            // Update score
            livingPlayer?.let { it.wins++ }
            scoreUpdated = true

            // Play sounds
            when
            {
                isLastLevel() ->
                {
                    engine.playSoundWithName(SOUND_CHEER, pitch = 2f)
                    engine.playSoundWithName(SOUND_VICTORY, pitch = 2f)
                }
                livingPlayer != null -> // One single player won
                {
                    engine.playSoundWithName(SOUND_CHEER, pitch = 2f)
                    engine.playSoundWithName(SOUND_WIN, pitch = 2f)
                }
                else -> engine.playSoundWithName(SOUND_DRAW, pitch = 2f)
            }
        }

        if (!transitionedToNextLevel && !isLastLevel() && elapsedGameOverTime > gameOverFadeTime + nextLevelTime)
        {
            transitionToNextLevel(engine)
            transitionedToNextLevel = true
        }
    }

    override fun onRender(engine: PulseEngine)
    {
        if (engine.scene.state != RUNNING)
            return

        val surface = engine.gfx.getSurface(OVERLAY_SURFACE) ?: return
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
                fontSize = fontSize,
                xOrigin = 0.5f,
                yOrigin = 0.5f
            )
        }

        if (gameOver)
        {
            // Full screen background
            val elapsedTime = System.currentTimeMillis() - gameOverTime
            val alpha = (elapsedTime.toFloat() / gameOverFadeTime).coerceIn(0f, 1f)
            surface.setDrawColor(BLACK, alpha * 0.6f)
            surface.drawTexture(BLANK, 0f, 0f, engine.window.width.toFloat(), engine.window.height.toFloat())

            // Winner text
            val sortedPlayers = engine.scene.getAllEntitiesOfType<Player>()?.sortedByDescending { it.wins * 100 + it.kills } ?: return
            val winningPlayer = getWinningPlayer(sortedPlayers)
            val showFinalWinner = isLastLevel() && !transitionedToNextLevel
            val color = if (showFinalWinner) GOLD else winningPlayer?.color ?: WHITE
            val text = if (winningPlayer == null) "DRAW" else "${winningPlayer.name} WINS!"
            val font = engine.asset.getOrNull<Font>(fontName)
            surface.setDrawColor(color, alpha)
            surface.drawText(text, engine.window.width / 2f, engine.window.height / 4f, font, fontSize = 180f, xOrigin = 0.5f, yOrigin = 0.5f)

            // Player stat cards
            val playerStatWidth = engine.window.width * 0.9f
            for ((i, player) in sortedPlayers.withIndex())
            {
                val step = playerStatWidth / sortedPlayers.size.toFloat()
                val x = 0.5f * engine.window.width - 0.5f * (playerStatWidth - step) + step * i
                val y = engine.window.height * 0.5f

                // Border color
                if (showFinalWinner)
                {
                    val borderColor = PODIUM_COLORS.getOrNull(i) ?: BLACK
                    surface.setDrawColor(borderColor, alpha)
                    surface.drawTexture(BLANK, x, y - 10f,  250f + 20, engine.window.height / 4f + 20, xOrigin = 0.5f, cornerRadius = 20f)
                }

                // Card background rect
                surface.setDrawColor(DARK, alpha * 0.8f)
                surface.drawTexture(BLANK, x, y,  250f, engine.window.height / 4f, xOrigin = 0.5f, cornerRadius = 20f)

                // Player name
                surface.setDrawColor(player.color, alpha)
                surface.drawText(player.name, x, y + 50, font, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 50f)

                // Player stats
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
        private val BLACK = Color(0f, 0f, 0f)
        private val GOLD = Color(255, 215, 0)
        private val SILVER = Color(192, 192, 192)
        private val BRONZE = Color(176, 141, 87)
        private val DARK = Color(0.2f, 0.2f, 0.2f)
        private var PODIUM_COLORS = listOf(GOLD, SILVER, BRONZE)
        private var currentLevelIndex = -1
        private val queuedLevelNames = (0 until LEVEL_PLAY_THROUGH_COUNT).flatMap { LEVELS.shuffled() }

        fun transitionToNextLevel(engine: PulseEngine)
        {
            if (currentLevelIndex < queuedLevelNames.lastIndex)
            {
                engine.scene.forEachEntityOfType<Player> { PlayerSpawnSystem.transferPlayer(it) }
                engine.scene.transitionInto(fileName = queuedLevelNames[++currentLevelIndex], fromClassPath = true, fadeTimeMs = 500)
            }
        }
    }
}