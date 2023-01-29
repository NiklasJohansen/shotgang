package entities

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.modules.scene.entities.StandardSceneEntity
import no.njoh.pulseengine.core.scene.SceneState

class SpawnPoint : StandardSceneEntity()
{
    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        if (engine.scene.state == SceneState.RUNNING)
            return

        surface.setDrawColor(1f, 0.8f, 0.4f)
        surface.drawTexture(Texture.BLANK, x, y, 20f, 20f, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = 10f)
    }
}