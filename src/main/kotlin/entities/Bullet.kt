package entities

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.annotations.Property
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.modules.physics.BodyType
import no.njoh.pulseengine.modules.physics.bodies.PointBody
import no.njoh.pulseengine.modules.physics.shapes.PointShape
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Bullet : SceneEntity(), PointBody
{
    // Properties
    var startColor = Color(1f, 0.9f, 1f)
    var endColor = Color(1f, 0.7f, 0.6f, 0.6f)
    var maxLifeTimeMillis = 200f
    var spawnerId = -1L

    // Physics
    @JsonIgnore override val shape = PointShape()
    @Property("Physics", 0) override var bodyType = BodyType.DYNAMIC
    @Property("Physics", 1) override var layerMask = 1
    @Property("Physics", 2) override var collisionMask = 1
    @Property("Physics", 3) override var restitution = 0f
    @Property("Physics", 4) override var density = 1f
    @Property("Physics", 5) override var friction = 0f
    @Property("Physics", 6) override var drag = 0.1f

    @JsonIgnore private var spawnTimeMillis = System.currentTimeMillis()
    @JsonIgnore private var xLastLast = 0f
    @JsonIgnore private var yLastLast = 0f
    @JsonIgnore private var framesInvisible = 2

    fun init(xInit: Float, yInit: Float, angleRad: Float, velocity: Float)
    {
        shape.init(xInit, yInit)
        shape.xLast = xInit - velocity * cos(angleRad)
        shape.yLast = yInit - velocity * sin(angleRad)
        xLastLast = shape.xLast
        yLastLast = shape.yLast
    }

    override fun onUpdate(engine: PulseEngine)
    {
        if (getLifeTimeMillis() > maxLifeTimeMillis)
            set(DEAD)
    }

    override fun onFixedUpdate(engine: PulseEngine)
    {
        xLastLast = shape.xLast
        yLastLast = shape.yLast
        framesInvisible--
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        if (framesInvisible > 0) return

        val i = (getLifeTimeMillis() / maxLifeTimeMillis).coerceIn(0f, 1f)
        surface.setDrawColor(
            red   = (1f - i) * startColor.red   + i * endColor.red,
            green = (1f - i) * startColor.green + i * endColor.green,
            blue  = (1f - i) * startColor.blue  + i * endColor.blue,
            alpha = (1f - i) * startColor.alpha + i * endColor.alpha
        )
        surface.drawLine(xLastLast, yLastLast, shape.xLast, shape.yLast)
        surface.drawLine(shape.xLast, shape.yLast, shape.x, shape.y)
    }

    fun getVelocity(): Float = sqrt(shape.xVel * shape.xVel + shape.yVel * shape.yVel)

    fun getLifeTimeMillis() : Long = System.currentTimeMillis() - spawnTimeMillis
}