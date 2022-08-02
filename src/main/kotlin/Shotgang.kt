import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.PulseEngineGame
import no.njoh.pulseengine.core.asset.types.Font
import no.njoh.pulseengine.core.asset.types.Sound
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.api.BlendFunction
import no.njoh.pulseengine.core.graphics.postprocessing.effects.BlurEffect
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.widgets.cli.CommandLine
import no.njoh.pulseengine.widgets.editor.SceneEditor
import no.njoh.pulseengine.widgets.profiler.Profiler
import util.BULLET_SURFACE
import util.LOBBY_LEVEL
import util.TEXTURE_WALK
import util.loadAll
import kotlin.random.Random

fun main() = PulseEngine.run(Shotgang::class)

class Shotgang : PulseEngineGame()
{
    override fun onCreate()
    {
        engine.widget.add(CommandLine(), Profiler(), SceneEditor())
        engine.console.runScript("startup.ps")

        // Load assets
        engine.asset.loadAll<Texture>("textures")
        engine.asset.loadAll<Font>("fonts")
        engine.asset.loadAll<Sound>("sound")
        engine.asset.loadSpriteSheet("textures/top_down_spritesheet.png", TEXTURE_WALK,5, 1)

        // Create separate graphics surface for bullets
        engine.gfx.createSurface(
            name = BULLET_SURFACE,
            camera = engine.gfx.mainCamera,
            zOrder = engine.gfx.mainSurface.context.zOrder - 1,
            blendFunction = BlendFunction.ADDITIVE,
            backgroundColor = Color(0f, 0f, 0f, 0f)
        ).addPostProcessingEffect(BlurEffect(radius = 0.024f, blurPasses = 1))

        // Load and start scene
        engine.scene.loadAndSetActive(LOBBY_LEVEL, fromClassPath = true)
        engine.scene.start()
    }

    override fun onUpdate() { }

    override fun onRender() { }

    override fun onDestroy()
    {
        if (engine.scene.state == SceneState.STOPPED)
            engine.scene.save()
    }
}