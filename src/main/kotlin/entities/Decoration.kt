package entities

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.primitives.Color
import shared.NormalMapped

class Decoration : SceneEntity(), NormalMapped
{
    override var normalMapName = ""
    var textureName = ""
    var color = Color(1f, 1f, 1f)
    var xTiling = 1f
    var yTiling = 1f

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        surface.setDrawColor(color)
        surface.drawTexture(
            texture = engine.asset.getOrNull(textureName) ?: Texture.BLANK,
            x = x,
            y = y,
            width = width,
            height = height,
            rot = rotation,
            xOrigin = 0.5f,
            yOrigin = 0.5f,
            uTiling = xTiling,
            vTiling = yTiling
        )
    }
}