package entities

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.annotations.Property
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.modules.physics.BodyType
import no.njoh.pulseengine.modules.physics.ContactResult
import no.njoh.pulseengine.modules.physics.bodies.PhysicsBody
import no.njoh.pulseengine.modules.physics.bodies.PointBody
import no.njoh.pulseengine.modules.physics.shapes.PointShape
import shared.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Bullet : SceneEntity(), PointBody
{
    // Properties
    var color = Color(1f, 0.85f, 0.4f)
    var maxLifeTimeMillis = 200f
    var spawnerId = -1L

    // Physics
    @JsonIgnore override val shape = PointShape()
    @Property("Physics", 0) override var bodyType = BodyType.DYNAMIC
    @Property("Physics", 1) override var layerMask = BULLET_LAYER
    @Property("Physics", 2) override var collisionMask = WALL_LAYER or PLAYER_LAYER
    @Property("Physics", 3) override var restitution = 0f
    @Property("Physics", 4) override var density = 1f
    @Property("Physics", 5) override var friction = 0f
    @Property("Physics", 6) override var drag = 0.1f

    @JsonIgnore private var spawnTimeMillis = System.currentTimeMillis()
    @JsonIgnore private var xLast = 0f
    @JsonIgnore private var yLast = 0f

    fun init(xInit: Float, yInit: Float, angleRad: Float, velocity: Float)
    {
        shape.init(xInit, yInit)
        shape.xLast = xInit - velocity * cos(angleRad)
        shape.yLast = yInit - velocity * sin(angleRad)
        xLast = xInit
        yLast = yInit
        x = xInit
        y = yInit
    }

    override fun onUpdate(engine: PulseEngine)
    {
        if (getLifeTimeMillis() > maxLifeTimeMillis)
            set(DEAD)
    }

    override fun onBodyUpdated()
    {
        xLast = x
        yLast = y
        x = shape.x
        y = shape.y
        set(POSITION_UPDATED)
    }

    override fun onCollision(engine: PulseEngine, otherBody: PhysicsBody, result: ContactResult)
    {
        if (otherBody is Player && otherBody.id != spawnerId)
            VfxFactory.spawnBloodSmokePuffEffect(engine, result.x, result.y, result.xNormal, result.yNormal)

        if (otherBody is SpikeWall)
            VfxFactory.spawnBulletWallHitEffects(engine, result.x, result.y, result.xNormal, result.yNormal)
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        val t = 1f - (getLifeTimeMillis() / maxLifeTimeMillis).coerceIn(0f, 1f)
        val alpha = if (t > 0.3f) 1f else t / 0.3f
        surface.setDrawColor(color, alpha)
        surface.drawLine(xLast, yLast, x, y)
    }

    fun getVelocity(): Float = sqrt(shape.xVel * shape.xVel + shape.yVel * shape.yVel)

    fun getLifeTimeMillis() : Long = System.currentTimeMillis() - spawnTimeMillis
}