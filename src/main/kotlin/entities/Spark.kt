package entities

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.Extensions.toDegrees
import no.njoh.pulseengine.modules.lighting.LightSource
import no.njoh.pulseengine.modules.lighting.LightType
import no.njoh.pulseengine.modules.lighting.ShadowType
import no.njoh.pulseengine.modules.physics.BodyType
import no.njoh.pulseengine.modules.physics.bodies.PointBody
import no.njoh.pulseengine.modules.physics.shapes.PointShape
import shared.*
import kotlin.math.*

class Spark : SceneEntity(), PointBody, LightSource
{
    // Appearance
    var textureName = TEXTURE_SPARK_0
    var startColor = Color(1f, 0.85f, 0.5f)
    var endColor = Color(1f, 0.5f, 0.0f)
    val thickness = 5f
    val minLength = 5f
    var timeToLiveMillis = 1000L

    // Physics
    @JsonIgnore override val shape = PointShape()
    override var bodyType = BodyType.DYNAMIC
    override var layerMask = PARTICLE_LAYER
    override var collisionMask = WALL_LAYER
    override var restitution = 0f
    override var density = 1f
    override var friction = 0f
    override var drag = 0.2f
    var turbulence = 0.3f

    // Lighting
    override var color = Color(1f, 1f, 1f)
    override var intensity = 4f
    override var radius = 400f
    override var size = 100f
    override var coneAngle = 360f
    override var spill = 0.95f
    override var type = LightType.RADIAL
    override var shadowType = ShadowType.HARD

    @JsonIgnore private var spawnTimeMillis = System.currentTimeMillis()
    @JsonIgnore private var initialIntensity = intensity
    @JsonIgnore private var xLast = 0f
    @JsonIgnore private var yLast = 0f

    init { setNot(DISCOVERABLE) }

    fun init(xInit: Float, yInit: Float, angleRad: Float, velocity: Float)
    {
        shape.init(xInit, yInit)
        shape.xLast = xInit - velocity * cos(angleRad)
        shape.yLast = yInit - velocity * sin(angleRad)
        xLast = xInit
        yLast = yInit
        x = xInit
        y = yInit
        z = -0.01f
    }

    override fun onStart(engine: PulseEngine)
    {
        initialIntensity = intensity
    }

    override fun onUpdate(engine: PulseEngine)
    {
        val lifeTime = getLifeTime()
        if (lifeTime > timeToLiveMillis)
            set(DEAD)

        val t = 1f - getLifeTimeFraction()
        intensity   = t * initialIntensity
        color.red   = t * startColor.red   + (1f - t) * endColor.red
        color.green = t * startColor.green + (1f - t) * endColor.green
        color.blue  = t * startColor.blue  + (1f - t) * endColor.blue
    }

    override fun onFixedUpdate(engine: PulseEngine)
    {
        val t = 1f - getLifeTimeFraction()
        shape.xLast += t * t * turbulence * nextRandomGaussian()
        shape.yLast += t * t * turbulence * nextRandomGaussian()
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        val xVel = x - xLast
        val yVel = y - yLast
        val length = max(minLength, sqrt(xVel * xVel + yVel * yVel))
        val angle = -atan2(yVel, xVel).toDegrees()
        val t = 1f - getLifeTimeFraction()
        val alpha = if (t > 0.5f) 1f else t / 0.5f

        surface.setDrawColor(color, alpha)
        surface.drawTexture(
            texture = engine.asset.getOrNull(textureName) ?: Texture.BLANK,
            x = (x + xLast) / 2f,
            y = (y + yLast) / 2f,
            width = length,
            height = thickness,
            rot = angle,
            xOrigin = 0.5f,
            yOrigin = 0.5f,
            cornerRadius = 5f
        )
    }

    override fun onBodyUpdated()
    {
        xLast = x
        yLast = y
        x = shape.x
        y = shape.y
        set(POSITION_UPDATED)
    }

    private fun getLifeTime() = System.currentTimeMillis() - spawnTimeMillis

    private fun getLifeTimeFraction() = (getLifeTime().toFloat() / timeToLiveMillis).coerceIn(0f, 1f)
}