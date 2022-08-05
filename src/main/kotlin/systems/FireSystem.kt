package systems

import com.fasterxml.jackson.annotation.JsonIgnore
import entities.Player
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.core.scene.SceneSystem
import shared.OVERLAY_SURFACE
import shared.TEMPLATE_NAME

class FireSystem : SceneSystem()
{
    var millisBetweenShots = 5000

    @JsonIgnore private var lastShotTime = 0L

    override fun onUpdate(engine: PulseEngine)
    {
        if (engine.scene.state != SceneState.RUNNING)
            return

        if (lastShotTime <= 0L && isGameStarted(engine))
            lastShotTime = System.currentTimeMillis()

        if (getProgress() == 1.0f && isMoreThanOnePlayerAlive(engine) && isGameStarted(engine))
        {
            engine.scene.getAllEntitiesOfType<Player>()?.forEachFast()
            {
                it.shoot(engine)
            }

            lastShotTime = System.currentTimeMillis()
        }
    }

    override fun onRender(engine: PulseEngine)
    {
        if (!isMoreThanOnePlayerAlive(engine) || !isGameStarted(engine))
            return

        val surface = engine.gfx.getSurface(OVERLAY_SURFACE) ?: return

        // Timer bar
        val p = getProgress()
        surface.setDrawColor(0.1f, 0.1f, 0.13f)
        surface.drawTexture(Texture.BLANK, 0f, 0f, engine.window.width.toFloat(), 20f)
        surface.setDrawColor(p, 1f - p, 0f)
        surface.drawTexture(Texture.BLANK, 0f, 0f, engine.window.width.toFloat() * (1f - p), 20f)
    }

    private fun getProgress() =
        ((System.currentTimeMillis() - lastShotTime) / millisBetweenShots.toFloat()).coerceIn(0f, 1f)

    private fun isMoreThanOnePlayerAlive(engine: PulseEngine): Boolean
    {
        val players = engine.scene.getAllEntitiesOfType<Player>() ?: return false
        val playersAlive = players.count { !it.isDead() && it.name != TEMPLATE_NAME }
        return (players.size > 1 && playersAlive > 1) || (players.size == 1 && playersAlive == 1)
    }

    private fun isGameStarted(engine: PulseEngine) =
        engine.scene.getSystemOfType<GameStateSystem>()?.gameStarted == true
}