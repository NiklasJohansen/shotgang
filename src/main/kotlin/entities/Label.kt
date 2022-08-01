package entities

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Font
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.primitives.Color

class Label : SceneEntity()
{
    var text = "TEXT"
    var font = ""
    var size = 20f
    var color = Color(1f, 1f, 1f)

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        val font = engine.asset.getOrNull(font) ?: Font.DEFAULT

        surface.setDrawColor(color)
        surface.drawText(text, x, y, xOrigin = 0.5f, yOrigin = 0.5f, font = font, fontSize = size)
    }
}