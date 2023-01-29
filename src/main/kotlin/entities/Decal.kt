package entities

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.shared.annotations.AssetRef
import no.njoh.pulseengine.modules.scene.entities.StandardSceneEntity
import no.njoh.pulseengine.core.shared.primitives.Color

class Decal : StandardSceneEntity()
{
    @AssetRef(Texture::class)
    var textureName = ""
    var color = Color(1f, 1f, 1f)

    init { setNot(DISCOVERABLE) }

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
            yOrigin = 0.5f
        )
    }
}