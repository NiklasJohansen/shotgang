package entities

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.core.shared.annotations.Property
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.Extensions.interpolateFrom
import no.njoh.pulseengine.modules.physics.BodyType
import no.njoh.pulseengine.modules.physics.bodies.CircleBody
import no.njoh.pulseengine.modules.physics.shapes.CircleShape
import util.SHELL_LAYER
import util.TEXTURE_SHOTGUN_SHELL
import util.WALL_LAYER
import kotlin.math.sqrt
import kotlin.random.Random

class Shell : SceneEntity(), CircleBody
{
    var color = Color(1f, 1f, 1f)
    var textureName = TEXTURE_SHOTGUN_SHELL

    @JsonIgnore override val shape = CircleShape()
    @Property("Physics", 0) override var bodyType = BodyType.DYNAMIC
    @Property("Physics", 1) override var layerMask = SHELL_LAYER
    @Property("Physics", 2) override var collisionMask = WALL_LAYER
    @Property("Physics", 3) override var restitution = 0f
    @Property("Physics", 4) override var density = 0f
    @Property("Physics", 5) override var friction = 0.2f
    @Property("Physics", 6) override var drag = 0.2f

    init { setNot(DISCOVERABLE) }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        val interpolate = (engine.scene.state == SceneState.RUNNING)
        val xPos = if (interpolate) x.interpolateFrom(shape.xLast) else x
        val yPos = if (interpolate) y.interpolateFrom(shape.yLast) else y

        surface.setDrawColor(color)
        surface.drawTexture(
            texture = engine.asset.getOrNull(textureName) ?: Texture.BLANK,
            x = xPos,
            y = yPos,
            width = width,
            height = height,
            rot = rotation,
            xOrigin = 0.5f,
            yOrigin = 0.5f
        )
    }
}