package entities

import util.PIf
import com.fasterxml.jackson.annotation.JsonIgnore
import util.filterStickInput
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.SpriteSheet
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.input.Axis
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.scene.SceneState.RUNNING
import no.njoh.pulseengine.core.shared.annotations.Property
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.Extensions.interpolateFrom
import no.njoh.pulseengine.core.shared.utils.Extensions.toRadians
import no.njoh.pulseengine.modules.lighting.LightSource
import no.njoh.pulseengine.modules.lighting.LightType
import no.njoh.pulseengine.modules.lighting.ShadowType
import no.njoh.pulseengine.modules.physics.BodyType
import no.njoh.pulseengine.modules.physics.ContactResult
import no.njoh.pulseengine.modules.physics.bodies.CircleBody
import no.njoh.pulseengine.modules.physics.bodies.PhysicsBody
import no.njoh.pulseengine.modules.physics.shapes.CircleShape
import systems.GameStateSystem
import util.playSoundWithName
import util.setDrawColor
import kotlin.math.*

class Player : SceneEntity(), CircleBody, LightSource
{
    var name = "unknown"
    var gamepadId = 0
    var life = 100f
    override var color = Color(1f, 1f, 1f)

    // Animation props
    @Property("Animation", 0) var textureName = ""
    @Property("Animation", 1) var textureScale = 1f
    @Property("Animation", 2) var frameStartIndex = 0
    @Property("Animation", 3) var frameEndIndex = 0
    @Property("Animation", 4) var frameRate = 2

    // Physics props
    @JsonIgnore override val shape = CircleShape()
    @Property("Physics", 0) override var bodyType = BodyType.DYNAMIC
    @Property("Physics", 1) override var layerMask = 1
    @Property("Physics", 2) override var collisionMask = 1
    @Property("Physics", 3) override var restitution = 0f
    @Property("Physics", 4) override var density = 1f
    @Property("Physics", 5) override var friction = 0.4f
    @Property("Physics", 6) override var drag = 0.1f
    @Property("Physics", 7) var speed = 800f

    // Lighting props
    @Property("Light", 1, 0f) override var intensity = 4f
    @Property("Light", 2, 0f) override var radius: Float = 800f
    @Property("Light", 3, 0f) override var size = 100f
    @Property("Light", 4, 0f, 360f) override var coneAngle = 360f
    @Property("Light", 5, 0f, 1f) override var spill: Float = 0.95f
    @Property("Light", 6) override var type = LightType.RADIAL
    @Property("Light", 7) override var shadowType = ShadowType.SOFT

    // Shooting props
    @Property("Shooting", 0) var shootingEnabled = false
    @Property("Shooting", 1) var fireRate = 10f
    @Property("Shooting", 2) var recoil = 1000f
    @Property("Shooting", 3) var fullAuto = false
    @Property("Shooting", 4) var bulletVelocity = 100f
    @Property("Shooting", 5) var bulletMass = 10f
    @Property("Shooting", 6) var bulletConeAngle = 45f
    @Property("Shooting", 7) var bulletSpreadAngle = 2f
    @Property("Shooting", 8) var bulletCount = 10
    @Property("Shooting", 9) var bulletSpawnOffsetAngle = 30f
    @Property("Shooting", 10) var bulletSpawnOffsetLength = 40f

    // Players state
    @JsonIgnore private var accRot = 0f
    @JsonIgnore private var accX = 0f
    @JsonIgnore private var accY = 0f
    @JsonIgnore private var frame = 0f
    @JsonIgnore private var shootTime = 0L
    @JsonIgnore private var shotFired = false

    // Scoring
    @JsonIgnore var kills = 0
    @JsonIgnore var wins = 0

    override fun onStart(engine: PulseEngine)
    {
        frame = frameStartIndex.toFloat()
    }

    override fun onUpdate(engine: PulseEngine)
    {
        accX = 0f
        accY = 0f
        accRot = 0f

        val gameStateSystem = engine.scene.getSystemOfType<GameStateSystem>()
        val hasGameStarted = gameStateSystem?.gameStarted != false

        if (!isDead() && gameStateSystem?.gameOver != true)
        {
            val gamepad = engine.input.gamepads.find { it.id == gamepadId } ?: return
            val (xLeft, yLeft) = filterStickInput(gamepad.getAxis(Axis.LEFT_X), gamepad.getAxis(Axis.LEFT_Y))
            val (xRight, yRight) = filterStickInput(gamepad.getAxis(Axis.RIGHT_X), gamepad.getAxis(Axis.RIGHT_Y))
            val rightTrigger = gamepad.getAxis(Axis.RIGHT_TRIGGER)

            // Rotation
            if (sqrt(xRight * xRight + yRight * yRight) > 0.4f)
            {
                val targetAngle = atan2(yRight, xRight)
                shape.rot = targetAngle
                shape.rotLast = targetAngle
            }

            if (hasGameStarted)
            {
                // Movement
                accX += speed * xLeft
                accY += speed * -yLeft

                handleShooting(engine, rightTrigger)
            }
        }
    }

    private fun handleShooting(engine: PulseEngine, rightTrigger: Float)
    {
        if (shootingEnabled && rightTrigger > 0.5)
        {
            if (!shotFired || fullAuto)
            {
                shoot(engine)
                shotFired = true
            }
        }
        else shotFired = false
    }

    fun shoot(engine: PulseEngine)
    {
        if (isDead()) return

        val time = System.currentTimeMillis()
        if (time - shootTime < 1000f / max(0.0001f, fireRate))
            return // To close to last shot

        // Apply recoil
        val recoilAngle = -shape.rot + PIf
        val recoilForce = shape.mass * recoil
        shape.xAcc += recoilForce * cos(recoilAngle)
        shape.yAcc += recoilForce * sin(recoilAngle)

        // Sound
        engine.playSoundWithName("shotgun")

        // Spawn bullets
        for (i in 0 until bulletCount)
        {
            // Calculate bullet spawn position
            val bulletAngle = -shape.rot
            val offsetSpawnAngle = bulletSpawnOffsetAngle.toRadians() + 0.02f * nextRandomFloat()
            val xBullet = x + cos(bulletAngle + offsetSpawnAngle) * bulletSpawnOffsetLength
            val yBullet = y + sin(bulletAngle + offsetSpawnAngle) * bulletSpawnOffsetLength

            // Calculate cone and spread angles
            val bulletConeAngle = bulletConeAngle.toRadians()
            val spreadAngle = bulletSpreadAngle.toRadians() * nextRandomFloat()
            val stepAngle = bulletConeAngle / bulletCount
            val coneAngle = 0.5f * (bulletConeAngle - stepAngle) - stepAngle * i

            // Spawn bullet
            val bullet = Bullet()
            bullet.init(xBullet, yBullet, bulletAngle + coneAngle + spreadAngle, bulletVelocity)
            bullet.shape.mass = bulletMass
            bullet.spawnerId = this.id
            bullet.layerMask = 1
            bullet.collisionMask = 1
            engine.scene.addEntity(bullet)

            // Muzzle flash
            if (i == 0)
            {
                val flash = Flash()
                flash.color = Color(1f, 0.85f, 0.3f)
                flash.lifeTimeMillis = 80f
                flash.coneAngle = 0f
                flash.intensity = 10f
                flash.radius = 600f
                flash.rotation = rotation
                flash.x = xBullet
                flash.y = yBullet
                flash.onStart(engine)
                engine.scene.addEntity(flash)
            }
        }

        shootTime = time
    }

    override fun onFixedUpdate(engine: PulseEngine)
    {
        // Physics
        wakeUp()
        shape.xAcc += accX
        shape.yAcc += accY
        shape.rotLast = shape.rot // Kill angular momentum

        // Animation
        if (abs(accX) > 0f || abs(accY) > 0f)
        {
            frame += engine.data.fixedDeltaTime * frameRate
            if (frame >= frameEndIndex)
                frame = frameStartIndex.toFloat()
        }
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        val interpolate = (engine.scene.state == RUNNING)
        val xPos = if (interpolate) x.interpolateFrom(shape.xLast) else x
        val yPos = if (interpolate) y.interpolateFrom(shape.yLast) else y
        val spriteSheet = engine.asset.getOrNull<SpriteSheet>(textureName) ?: return
        val frameTex = spriteSheet.getTexture(frame.toInt())

        // entities.Player
        surface.setDrawColor(color, alpha = if (life <= 0) 0.6f else 1f)
        surface.drawTexture(
            texture = frameTex,
            x = xPos,
            y = yPos,
            width = width * textureScale,
            height = height * textureScale,
            rot = rotation + 90,
            xOrigin = 0.5f,
            yOrigin = 0.5f
        )

        // Life bar
        val a = life / 100f
        val barWidth = width * 0.8f
        val yBar = y - height * 0.8f
        surface.setDrawColor(0.3f, 0.3f, 0.3f, 0.5f)
        surface.drawTexture(Texture.BLANK, x - barWidth * 0.5f, yBar, barWidth, 4f)
        surface.setDrawColor(1f - a, a, 0f)
        surface.drawTexture(Texture.BLANK, x - barWidth * 0.5f, yBar, barWidth * a, 4f)

        // Name
        surface.setDrawColor(1f, 1f, 1f )
        surface.drawText(name, x, y - height * 1.01f, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 24f)
    }

    override fun onCollision(engine: PulseEngine, otherBody: PhysicsBody, result: ContactResult)
    {
        when (otherBody)
        {
            is Bullet -> {
                // Should not be hit by own bullets
                if (otherBody.spawnerId == this.id)
                    return

                // Only take damage if a GameStateSystem is present
                val takeDamage = engine.scene.getSystemOfType<GameStateSystem>() != null
                if (takeDamage)
                {
                    val dropOff = otherBody.getLifeTimeMillis() / otherBody.maxLifeTimeMillis
                    val damage = 0.5f * otherBody.getVelocity() * otherBody.getMass() * dropOff

                    if (life > 0 && life - damage <= 0)
                        engine.scene.getEntityOfType<Player>(otherBody.spawnerId)?.let { it.kills++ }

                    life -= damage
                    if (life <= 0f)
                        setDead(engine)
                }
            }

            is SpikeWall -> {
                if (otherBody.hasSpikes) setDead(engine)
            }
        }
    }

    @JsonIgnore
    fun setDead(engine: PulseEngine)
    {
        life = 0f
        bodyType = BodyType.STATIC
        layerMask = 128
        shape.xLast = x
        shape.yLast = y
        engine.playSoundWithName("death")
    }

    fun setAlive()
    {
        life = 100f
        bodyType = BodyType.DYNAMIC
        layerMask = 1
        shape.xLast = x
        shape.yLast = y
    }

    @JsonIgnore
    fun isDead() = life <= 0f

    fun copy(): Player
    {
        val p = Player()
        p.x = x
        p.y = y
        p.z = z
        p.rotation = rotation
        p.width = width
        p.height = height
        p.name = name
        p.gamepadId = gamepadId
        p.color = color
        p.life = life

        // Animation
        p.textureName = textureName
        p.textureScale = textureScale
        p.frameStartIndex = frameStartIndex
        p.frameEndIndex = frameEndIndex
        p.frameRate = frameRate

        // Physics
        p.bodyType = bodyType
        p.layerMask = layerMask
        p.collisionMask = collisionMask
        p.restitution = restitution
        p.density = density
        p.friction = friction
        p.drag = drag
        p.speed = speed

        // Shooting
        p.shootingEnabled = shootingEnabled
        p.fireRate = fireRate
        p.recoil = recoil
        p.fullAuto = fullAuto
        p.bulletVelocity = bulletVelocity
        p.bulletMass = bulletMass
        p.bulletConeAngle = bulletConeAngle
        p.bulletSpreadAngle = bulletSpreadAngle
        p.bulletCount = bulletCount
        p.bulletSpawnOffsetAngle = bulletSpawnOffsetAngle
        p.bulletSpawnOffsetLength = bulletSpawnOffsetLength

        // Scoring
        p.kills = kills
        p.wins = wins

        return p
    }

    companion object
    {
        private val random = java.util.Random()
        fun nextRandomFloat() = random.nextGaussian().toFloat()
    }
}