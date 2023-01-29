package entities

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.modules.scene.entities.StandardSceneEntity
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.core.shared.annotations.AssetRef
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.Extensions.interpolateFrom
import no.njoh.pulseengine.modules.physics.BodyType
import no.njoh.pulseengine.modules.physics.bodies.CircleBody
import no.njoh.pulseengine.modules.physics.shapes.CircleShape
import shared.*

class Shell : StandardSceneEntity(), CircleBody
{
    @AssetRef(Texture::class)
    var textureName = TEXTURE_SHOTGUN_SHELL
    var color = Color(1f, 1f, 1f)

    @JsonIgnore
    override val shape = CircleShape()
    override var bodyType = BodyType.DYNAMIC
    override var layerMask = SHELL_LAYER
    override var collisionMask = WALL_LAYER
    override var restitution = 0f
    override var density = 0f
    override var friction = 0.2f
    override var drag = 0.2f

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