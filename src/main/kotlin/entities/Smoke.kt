package entities

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.annotations.Property
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.modules.physics.BodyType
import no.njoh.pulseengine.modules.physics.bodies.CircleBody
import no.njoh.pulseengine.modules.physics.shapes.CircleShape
import shared.NO_COLLISION_LAYER
import shared.PARTICLE_LAYER
import shared.TEXTURE_SMOKE_PUFF
import shared.setDrawColor

class Smoke : SceneEntity(), CircleBody
{
    var color = Color(1f, 1f, 1f)
    var textureName = TEXTURE_SMOKE_PUFF
    var lifeTimeMillis = 200L
    var startSize = 10f
    var endSize = 300f

    @Property("Physics", 0) override var bodyType = BodyType.DYNAMIC
    @Property("Physics", 1) override var layerMask = PARTICLE_LAYER
    @Property("Physics", 2) override var collisionMask = NO_COLLISION_LAYER
    @Property("Physics", 3) override var restitution = 0f
    @Property("Physics", 4) override var density = 1f
    @Property("Physics", 5) override var friction = 0.2f
    @Property("Physics", 6) override var drag = 0.4f

    @JsonIgnore override val shape = CircleShape()
    @JsonIgnore var spawnTime = System.currentTimeMillis()

    init { setNot(DISCOVERABLE) }

    override fun onUpdate(engine: PulseEngine)
    {
        val elapsedTime = System.currentTimeMillis() - spawnTime
        if (elapsedTime > lifeTimeMillis)
            set(DEAD)

        val t = 1f - (elapsedTime / lifeTimeMillis.toFloat()).coerceIn(0f, 1f)
        val t0 = t * t
        val size = t0 * startSize + endSize * (1f - t0)

        width = size
        height = size
        shape.radius = size / 2
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        val elapsedTime = System.currentTimeMillis() - spawnTime
        val alpha = 1f - (elapsedTime / lifeTimeMillis.toFloat()).coerceIn(0f, 1f)

        surface.setDrawColor(color, alpha * alpha)
        surface.drawTexture(
            texture = engine.asset.getOrNull(textureName) ?: Texture.BLANK,
            x = x,
            y = y,
            width = width,
            height = height,
            rot = rotation,
            xOrigin = 0.5f,
            yOrigin = 0.5f
        )
    }
}