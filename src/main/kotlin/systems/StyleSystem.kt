package systems

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.scene.SceneSystem
import no.njoh.pulseengine.core.shared.primitives.Color

class StyleSystem : SceneSystem()
{
    var backgroundColor = Color(0.1f, 0.1f, 0.15f, 1f)

    override fun onUpdate(engine: PulseEngine)
    {
        engine.gfx.mainSurface.setBackgroundColor(backgroundColor)
    }
}