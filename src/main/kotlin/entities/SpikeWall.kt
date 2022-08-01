package entities

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.modules.lighting.LightOccluder
import no.njoh.pulseengine.modules.physics.entities.Box

class SpikeWall : Box(), LightOccluder
{
    var color = Color(0.3f, 0.3f, 0.3f)
    var textureName: String = ""
    var textureXScale = 1f
    var textureYScale = 1f
    var hasSpikes = true

    override var castShadows = true

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        val x = if (xInterpolated.isNaN()) x else xInterpolated
        val y = if (yInterpolated.isNaN()) y else yInterpolated
        val r = if (rotInterpolated.isNaN()) rotation else rotInterpolated

        surface.setDrawColor(color)
        surface.drawTexture(
            texture = engine.asset.getOrNull(textureName) ?: Texture.BLANK,
            x = x,
            y = y,
            width = width * textureXScale,
            height = height * textureYScale,
            rot = r, xOrigin = 0.5f, yOrigin = 0.5f)
    }
}