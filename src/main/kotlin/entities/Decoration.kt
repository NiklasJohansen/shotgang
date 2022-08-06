package entities

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.annotations.Property
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.modules.lighting.NormalMapRenderer.Orientation
import no.njoh.pulseengine.modules.lighting.NormalMapped

class Decoration : SceneEntity(), NormalMapped
{
    var textureName = ""
    var color = Color(1f, 1f, 1f)
    var xTiling = 1f
    var yTiling = 1f

    @Property("Lighting", 0) override var normalMapName = ""
    @Property("Lighting", 1) override var normalMapIntensity = 1f
    @Property("Lighting", 2) override var normalMapOrientation = Orientation.NORMAL

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