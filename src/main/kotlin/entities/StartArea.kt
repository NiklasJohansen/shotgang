package entities

import com.fasterxml.jackson.annotation.JsonIgnore
import systems.PlayerSpawnSystem
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.primitives.Color
import systems.GameStateSystem
import util.playSoundWithName
import kotlin.math.max

class StartArea : SceneEntity()
{
    var color = Color(0f, 1f, 0f, 0.8f)
    var textureName = ""
    var readyCountdownTime = 3000L
    var fontName = ""
    var fontSize = 200f
    var fontColor = Color(1f, 0.67f, 0f)

    @JsonIgnore private var startCountDownTime: Long? = 0L
    @JsonIgnore private var lastCountDownSecond = -1
    @JsonIgnore private var transitionedToNextLevel = false

    override fun onUpdate(engine: PulseEngine)
    {
        val spawnSystem = engine.scene.getSystemOfType<PlayerSpawnSystem>() ?: return
        var allPlayersReady = (spawnSystem.activePlayers.size > 0)

        for (playerId in spawnSystem.activePlayers)
        {
            val player = engine.scene.getEntityOfType<Player>(playerId) ?: continue
            if (player.x < x - width / 2 || player.x > x + width / 2 || player.y < y - height / 2 || player.y > y + height / 2)
            {
                allPlayersReady = false
                break
            }
        }

        startCountDownTime = if (allPlayersReady) startCountDownTime ?: System.currentTimeMillis() else null

        startCountDownTime?.let()
        {
            val elapsedTime = System.currentTimeMillis() - it
            val second = getCountDownSecond(it)
            if (second != lastCountDownSecond)
            {
                val soundName = if (second == 0) "countdown_ding_1" else "countdown_ding_0"
                engine.playSoundWithName(soundName)
                lastCountDownSecond = second
            }

            if (elapsedTime > readyCountdownTime && !transitionedToNextLevel)
            {
                GameStateSystem.transitionToNextLevel(engine)
                transitionedToNextLevel = true
            }
        }
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        surface.setDrawColor(color)
        surface.drawTexture(engine.asset.getOrNull(textureName) ?: Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f)

        startCountDownTime?.let()
        {
            val overlaySurface = engine.gfx.getSurface("overlay") ?: return
            overlaySurface.setDrawColor(fontColor)
            overlaySurface.drawText(
                text = getCountDownSecond(it).toString(),
                x = engine.window.width.toFloat() / 2f,
                y = engine.window.height.toFloat() / 2f,
                font = engine.asset.getOrNull(fontName),
                fontSize = fontSize, xOrigin = 0.5f, yOrigin = 0.5f)
        }
    }

    private fun getCountDownSecond(startCountDownTime: Long): Int
    {
        val elapsedTime = System.currentTimeMillis() - startCountDownTime
        return max(0, (readyCountdownTime / 1000) - (elapsedTime/1000)).toInt()
    }
}
