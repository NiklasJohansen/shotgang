package entities

import systems.PlayerSpawnSystem
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Font
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.input.Key
import no.njoh.pulseengine.modules.scene.entities.StandardSceneEntity
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.core.shared.annotations.AssetRef
import no.njoh.pulseengine.core.shared.primitives.Color
import systems.GameStateSystem
import shared.*
import kotlin.math.max

class StartArea : StandardSceneEntity()
{
    @AssetRef(Texture::class)
    var textureName = ""
    var color = Color(0f, 1f, 0f, 0.8f)

    @AssetRef(Font::class)
    var fontName =  FONT_BADABB
    var fontSize = 200f
    var fontColor = Color(1f, 0.67f, 0f)
    var readyCountdownTime = 3000L

    private var startCountDownTime: Long? = 0L
    private var lastCountDownSecond = -1
    private var transitionedToNextLevel = false

    override fun onUpdate(engine: PulseEngine)
    {
        val spawnSystem = engine.scene.getSystemOfType<PlayerSpawnSystem>() ?: return
        var allPlayersReady = isAllPlayersReady(engine, spawnSystem.activePlayers)

        // Manual override
        if (engine.input.isPressed(Key.R))
            allPlayersReady = true

        // Start count down when all players are ready
        startCountDownTime = if (allPlayersReady) startCountDownTime ?: System.currentTimeMillis() else null

        if (startCountDownTime != null)
        {
            val elapsedTime = System.currentTimeMillis() - startCountDownTime!!
            if (elapsedTime > readyCountdownTime && !transitionedToNextLevel)
            {
                GameStateSystem.transitionToNextLevel(engine)
                transitionedToNextLevel = true
            }
            playCountDownSound(engine, startCountDownTime!!)
        }
    }

    private fun isAllPlayersReady(engine: PulseEngine, activePlayerIds: List<Long>): Boolean
    {
        for (playerId in activePlayerIds)
        {
            val player = engine.scene.getEntityOfType<Player>(playerId) ?: continue
            if (player.x < x - width / 2 || player.x > x + width / 2 || player.y < y - height / 2 || player.y > y + height / 2)
                return false
        }
        return activePlayerIds.isNotEmpty()
    }

    private fun playCountDownSound(engine: PulseEngine, startCountDownTime: Long)
    {
        val second = getCountDownSecond(startCountDownTime)
        if (second != lastCountDownSecond)
        {
            val soundName = if (second == 0) SOUND_DING_1 else SOUND_DING_0
            engine.playSoundWithName(soundName)
            lastCountDownSecond = second
        }
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        // Start area rectangle
        surface.setDrawColor(color)
        surface.drawTexture(engine.asset.getOrNull(textureName) ?: Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f)

        // Count down text
        val countDownTime = startCountDownTime
        if (engine.scene.state == SceneState.RUNNING && countDownTime != null)
        {
            val overlaySurface = engine.gfx.getSurface(OVERLAY_SURFACE) ?: return
            overlaySurface.setDrawColor(fontColor)
            overlaySurface.drawText(
                text = getCountDownSecond(countDownTime).toString(),
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
