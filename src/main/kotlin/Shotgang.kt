import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.PulseEngineGame
import no.njoh.pulseengine.core.asset.types.Font
import no.njoh.pulseengine.core.asset.types.Sound
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.widgets.cli.CommandLine
import no.njoh.pulseengine.widgets.editor.SceneEditor
import no.njoh.pulseengine.widgets.profiler.Profiler
import util.LOBBY_LEVEL
import util.TEXTURE_WALK
import util.loadAll

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