package entities

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Font
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.primitives.Color
import shared.FONT_BADABB
import shared.NormalMapped

class Label : SceneEntity(), NormalMapped
{
    var text = "TEXT"
    var font = FONT_BADABB
    var size = 20f
    var color = Color(1f, 1f, 1f)

    override var normalMapName = ""

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        val font = engine.asset.getOrNull(font) ?: Font.DEFAULT
        surface.setDrawColor(color)
        surface.drawText(text, x, y, font, size, xOrigin = 0.5f, yOrigin = 0.5f)
    }

    override fun renderCustomPass(engine: PulseEngine, surface: Surface2D)
    {
        val font = engine.asset.getOrNull(font) ?: Font.DEFAULT
        surface.setDrawColor(0.5f, 0.5f, 1f)
        surface.drawText(text, x, y, font, size, xOrigin = 0.5f, yOrigin = 0.5f)
    }
}