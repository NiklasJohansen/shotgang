package entities

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.shared.annotations.Property
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.modules.lighting.LightOccluder
import no.njoh.pulseengine.modules.physics.entities.Box
import shared.NO_COLLISION_LAYER
import shared.NormalMapped
import shared.WALL_LAYER
import systems.EntityRenderSystem.DecalMask

class SpikeWall : Box(), LightOccluder, NormalMapped, DecalMask
{
    var color = Color(0.3f, 0.3f, 0.3f)
    var textureName: String = ""
    var textureXScale = 1f
    var textureYScale = 1f
    var hasSpikes = true

    @Property("Physics", 1) override var layerMask = WALL_LAYER
    @Property("Physics", 2) override var collisionMask = NO_COLLISION_LAYER
    @Property("Lighting", 1) override var castShadows = true
    @Property("Lighting", 2) override var normalMapName = ""

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        surface.setDrawColor(color)
        surface.drawTexture(
            texture = engine.asset.getOrNull(textureName) ?: Texture.BLANK,
            x = x,
            y = y,
            width = width * textureXScale,
            height = height * textureYScale,
            rot = rotation,
            xOrigin = 0.5f,
            yOrigin = 0.5f
        )
    }

    override fun renderCustomPass(engine: PulseEngine, surface: Surface2D)
    {
        if (normalMapName.isBlank())
            return

        val normalMap = engine.asset.getOrNull<Texture>(normalMapName)
        val dir = if (normalMap != null) 1.0f else 0.5f
        surface.setDrawColor(dir, dir, 1f)
        surface.drawTexture(
            texture = normalMap ?: Texture.BLANK,
            x = x,
            y = y,
            width = width * textureXScale,
            height = height * textureYScale,
            rot = rotation,
            xOrigin = 0.5f,
            yOrigin = 0.5f
        )
    }
}