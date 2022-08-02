package entities

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.annotations.Property
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.MathUtil.atan2
import no.njoh.pulseengine.modules.physics.BodyType
import no.njoh.pulseengine.modules.physics.ContactResult
import no.njoh.pulseengine.modules.physics.bodies.PhysicsBody
import no.njoh.pulseengine.modules.physics.bodies.PointBody
import no.njoh.pulseengine.modules.physics.shapes.PointShape
import util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

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
        {
            // Blood puff
            val blood = Smoke()
            blood.color = Color(1f, 0.4f + 0.3f * Random.nextFloat(), 0.4f + 0.3f * Random.nextFloat())
            blood.x = result.x
            blood.y = result.y
            blood.rotation = Random.nextFloat() * 360f
            blood.startSize = 10f + 15f * Random.nextFloat()
            blood.endSize = 40f + 40f * Random.nextFloat()
            blood.lifeTimeMillis = 500 + (800 * Random.nextFloat()).toLong()
            blood.init(engine)
            blood.shape.xLast = result.x - 3 * result.xNormal
            blood.shape.yLast = result.y - 3 * result.yNormal
            blood.shape.applyAngularAcceleration(0.03f * Random.nextFloat())
            engine.scene.addEntity(blood)
        }

        if (otherBody is SpikeWall)
        {
            // Smoke
            val smoke = Smoke()
            val velocity = 5f + 15f * Random.nextFloat()
            smoke.x = result.x
            smoke.y = result.y
            smoke.rotation = Random.nextFloat() * 360f
            smoke.startSize = 2f + 5f * Random.nextFloat()
            smoke.endSize = 30f + 20f * Random.nextFloat()
            smoke.lifeTimeMillis = 500 + (800 * Random.nextFloat()).toLong()
            smoke.init(engine)
            smoke.shape.applyAngularAcceleration(0.03f * Random.nextFloat())
            smoke.shape.xLast = result.x - velocity * result.xNormal
            smoke.shape.yLast = result.y - velocity * result.yNormal
            engine.scene.addEntity(smoke)

            // Flash
            val flash = Flash()
            flash.color = Color(1f, 0.7f, 0.3f)
            flash.lifeTimeMillis = 100f
            flash.coneAngle = 360f
            flash.intensity = 1f
            flash.radius = 300f
            flash.x = result.x
            flash.y = result.y
            flash.onStart(engine)
            engine.scene.addEntity(flash)

            for (i in 0 until 2)
            {
                val spark = Spark()
                val sparkVelocity = 30f + Random.nextFloat() * 10f
                val sparkAngle = atan2(result.yNormal, result.xNormal) + 0.5f * PIf * nextRandomGaussian()
                spark.turbulence = 10f
                spark.drag = 0.3f
                spark.timeToLiveMillis = 100 + Random.nextLong(100)
                spark.init(result.x, result.y, sparkAngle, sparkVelocity)
                spark.onStart(engine)
                engine.scene.addEntity(spark)
            }
        }
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        val t = 1f - (getLifeTimeMillis() / maxLifeTimeMillis).coerceIn(0f, 1f)
        val alpha = if (t > 0.3f) 1f else t / 0.3f
        val particleSurface = engine.gfx.getSurface(BULLET_SURFACE) ?: surface

        particleSurface.setDrawColor(color, alpha)
        particleSurface.drawLine(xLast, yLast, x, y)
    }

    fun getVelocity(): Float = sqrt(shape.xVel * shape.xVel + shape.yVel * shape.yVel)

    fun getLifeTimeMillis() : Long = System.currentTimeMillis() - spawnTimeMillis
}