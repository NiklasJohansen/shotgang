package entities

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Font
import no.njoh.pulseengine.core.asset.types.SpriteSheet
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.input.Axis
import no.njoh.pulseengine.modules.scene.entities.StandardSceneEntity
import no.njoh.pulseengine.core.scene.SceneState.RUNNING
import no.njoh.pulseengine.core.shared.annotations.AssetRef
import no.njoh.pulseengine.core.shared.annotations.ScnProp
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.Extensions.interpolateFrom
import no.njoh.pulseengine.core.shared.utils.Extensions.toRadians
import no.njoh.pulseengine.core.shared.utils.MathUtil.atan2
import no.njoh.pulseengine.modules.lighting.*
import no.njoh.pulseengine.modules.lighting.NormalMapRenderer.Orientation
import no.njoh.pulseengine.modules.physics.BodyType
import no.njoh.pulseengine.modules.physics.ContactResult
import no.njoh.pulseengine.modules.physics.bodies.CircleBody
import no.njoh.pulseengine.modules.physics.bodies.PhysicsBody
import no.njoh.pulseengine.modules.physics.shapes.CircleShape
import systems.GameStateSystem
import shared.*
import kotlin.math.*
import kotlin.random.Random

class Player : StandardSceneEntity(), CircleBody, LightSource, NormalMapped
{
    override var name = "unknown"
    var gamepadId = 0
    var life = FULL_LIFE

    // Graphics props
    @AssetRef(Texture::class)
    @ScnProp("Graphics", 0) var textureName = ""
    @ScnProp("Graphics", 1) var textureScale = 1f
    @ScnProp("Graphics", 2) var frameStartIndex = 0
    @ScnProp("Graphics", 3) var frameEndIndex = 0
    @ScnProp("Graphics", 4) var frameRate = 2
    @ScnProp("Graphics", 5) override var color = Color(1f, 1f, 1f)

    // Physics props
    @JsonIgnore
    override val shape = CircleShape()
    override var bodyType = BodyType.DYNAMIC
    override var layerMask = PLAYER_LAYER
    override var collisionMask = PLAYER_LAYER or WALL_LAYER
    override var restitution = 0f
    override var density = 1f
    override var friction = 0.4f
    override var drag = 0.1f
    @ScnProp("Physics", 7)
    var speed = 800f

    // Lighting props
    override var intensity = 4f
    override var radius: Float = 800f
    override var size = 100f
    override var coneAngle = 360f
    override var spill: Float = 0.95f
    override var type = LightType.RADIAL
    override var shadowType = ShadowType.SOFT
    @ScnProp("Lighting", 6) override var normalMapName = ""
    @ScnProp("Lighting", 8) override var normalMapIntensity = 1f
    @ScnProp("Lighting", 9) override var normalMapOrientation = Orientation.NORMAL

    // Shooting props
    @ScnProp("Shooting", 0) var shootingEnabled = false
    @ScnProp("Shooting", 1) var fireRate = 10f
    @ScnProp("Shooting", 2) var recoil = 1000f
    @ScnProp("Shooting", 3) var fullAuto = false
    @ScnProp("Shooting", 4) var bulletVelocity = 100f
    @ScnProp("Shooting", 5) var bulletMass = 10f
    @ScnProp("Shooting", 6) var bulletConeAngle = 45f
    @ScnProp("Shooting", 7) var bulletSpreadAngle = 2f
    @ScnProp("Shooting", 8) var bulletCount = 10
    @ScnProp("Shooting", 9) var bulletSpawnOffsetAngle = 30f
    @ScnProp("Shooting", 10) var bulletSpawnOffsetLength = 40f

    // Players state
    private var accRot = 0f
    private var xAcc = 0f
    private var yAcc = 0f
    private var frame = 0f
    private var shootTime = 0L
    private var shotFired = false
    private var lightIntensity = intensity
    private var lastStepTime = 0L

    // Scoring
    @JsonIgnore var kills = 0
    @JsonIgnore var wins = 0

    override fun onStart(engine: PulseEngine)
    {
        frame = frameStartIndex.toFloat()
    }

    override fun onUpdate(engine: PulseEngine)
    {
        xAcc = 0f
        yAcc = 0f
        accRot = 0f

        val gameStateSystem = engine.scene.getSystemOfType<GameStateSystem>()
        val hasGameStarted = gameStateSystem?.gameStarted != false
        val isGameOver = gameStateSystem?.gameOver == true

        if (!isDead() && !isGameOver)
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
                xAcc += speed * xLeft
                yAcc += speed * -yLeft

                handleFootsteps(engine)
                handleShooting(engine, rightTrigger)
            }
        }

        // Disable player light on death
        intensity = if (isDead()) 0f else lightIntensity
    }

    override fun onFixedUpdate(engine: PulseEngine)
    {
        // Physics
        wakeUp()
        shape.xAcc += xAcc
        shape.yAcc += yAcc
        shape.rotLast = shape.rot // Kill angular momentum

        // Animation
        if (abs(xAcc) > 0f || abs(yAcc) > 0f)
        {
            frame += engine.data.fixedDeltaTime * frameRate
            if (frame >= frameEndIndex)
                frame = frameStartIndex.toFloat()
        }
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        // Player
        val spriteSheet = engine.asset.getOrNull<SpriteSheet>(textureName) ?: return
        surface.setDrawColor(color, alpha = if (life <= 0) 0.6f else 1f)
        surface.drawTexture(
            texture = spriteSheet.getTexture(frame.toInt()),
            x = if (engine.scene.state == RUNNING) x.interpolateFrom(shape.xLast) else x,
            y = if (engine.scene.state == RUNNING) y.interpolateFrom(shape.yLast) else y,
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

        // Name tag
        val font = engine.asset.getOrNull<Font>(FONT_BADABB)
        surface.setDrawColor(1f, 1f, 1f )
        surface.drawText(name, x, y - height * 0.85f, font, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 22f)
    }

    override fun renderCustomPass(engine: PulseEngine, surface: Surface2D)
    {
        if (normalMapName.isBlank())
            return

        surface.getRenderer(NormalMapRenderer::class)?.drawNormalMap(
            texture = engine.asset.getOrNull<SpriteSheet>(normalMapName)?.getTexture(frame.toInt()),
            x = if (engine.scene.state == RUNNING) x.interpolateFrom(shape.xLast) else x,
            y = if (engine.scene.state == RUNNING) y.interpolateFrom(shape.yLast) else y,
            w = width * textureScale,
            h = height * textureScale,
            rot = rotation + 90,
            xOrigin = 0.5f,
            yOrigin = 0.5f,
            normalScale = normalMapIntensity,
            orientation = normalMapOrientation
        )
    }

    override fun onCollision(engine: PulseEngine, otherBody: PhysicsBody, result: ContactResult)
    {
        when (otherBody)
        {
            is Bullet ->
            {
                // Should not be hit by own bullets
                if (otherBody.spawnerId == this.id)
                    return

                // Only take damage if a GameStateSystem is present
                val takeDamage = engine.scene.getSystemOfType<GameStateSystem>() != null
                if (takeDamage)
                {
                    val dropOff = otherBody.getLifeTimeMillis() / otherBody.maxLifeTimeMillis
                    val damage = 0.30f * otherBody.getVelocity() * otherBody.getMass() * dropOff

                    if (life > 0 && life - damage <= 0)
                        engine.scene.getEntityOfType<Player>(otherBody.spawnerId)?.let { it.kills++ }

                    life -= damage
                    if (life <= 0f)
                        setDead(engine)
                }

                // Blood decal on walls
                VfxFactory.spawnWallBloodEffect(engine, x + otherBody.shape.xVel, y + otherBody.shape.yVel)

                // Blood decal on ground
                if (Random.nextFloat() < 0.2)
                    VfxFactory.spawnGroundBloodEffect(engine, x, y)
            }

            is SpikeWall ->
            {
                if (otherBody.hasSpikes)
                {
                    setDead(engine)
                    VfxFactory.spawnSpikeWallBloodEffect(engine, x, y)
                }
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
        if (isDead() || System.currentTimeMillis() - shootTime < 1000f / max(0.0001f, fireRate))
            return // Dead or too close to last shot

        // Apply recoil
        val recoilAngle = -shape.rot + PIf
        shape.xAcc += recoil * cos(recoilAngle)
        shape.yAcc += recoil * sin(recoilAngle)

        // Sound
        engine.playSoundWithName(SOUND_SHOTGUN, pitch = 1f + 0.05f * nextRandomGaussian(),)
        engine.playSoundWithName(
            name = listOf(SOUND_SHELL_DROP_0, SOUND_SHELL_DROP_1, SOUND_SHELL_DROP_2).random(),
            pitch = 2f + 0.2f * nextRandomGaussian(),
            volume = 0.5f
        )

        // Spawn bullets
        var xBullet = 0f
        var yBullet = 0f
        for (i in 0 until bulletCount)
        {
            // Calculate bullet spawn position
            val bulletAngle = -shape.rot
            val offsetSpawnAngle = bulletSpawnOffsetAngle.toRadians() + 0.02f * nextRandomGaussian()
            xBullet = x + cos(bulletAngle + offsetSpawnAngle) * bulletSpawnOffsetLength
            yBullet = y + sin(bulletAngle + offsetSpawnAngle) * bulletSpawnOffsetLength

            // Calculate cone and spread angles
            val bulletConeAngle = bulletConeAngle.toRadians()
            val spreadAngle = bulletSpreadAngle.toRadians() * nextRandomGaussian()
            val stepAngle = bulletConeAngle / bulletCount
            val coneAngle = 0.5f * (bulletConeAngle - stepAngle) - stepAngle * i

            // Spawn bullet
            val bullet = Bullet()
            bullet.init(xBullet, yBullet, bulletAngle + coneAngle + spreadAngle, bulletVelocity)
            bullet.shape.mass = bulletMass
            bullet.spawnerId = this.id
            engine.scene.addEntity(bullet)
        }

        // VFX
        VfxFactory.spawnShotgunEffects(engine, xBullet, yBullet, rotation)

        shootTime = System.currentTimeMillis()
    }

    private fun handleFootsteps(engine: PulseEngine)
    {
        val millisBetweenSteps = 250 + Random.nextInt(3)
        if (sqrt(xAcc * xAcc + yAcc * yAcc) > 1f && System.currentTimeMillis() - lastStepTime > millisBetweenSteps)
        {
            engine.playSoundWithName(
                name = listOf(SOUND_STEP_0, SOUND_STEP_1, SOUND_STEP_2).random(),
                pitch = 2f + 0.1f * nextRandomGaussian(),
                volume = 0.3f + 0.1f * nextRandomGaussian()
            )
            lastStepTime = System.currentTimeMillis()
        }
    }

    @JsonIgnore
    fun setDead(engine: PulseEngine)
    {
        life = 0f
        bodyType = BodyType.STATIC
        layerMask = NO_COLLISION_LAYER
        shape.xLast = x
        shape.yLast = y
        engine.playSoundWithName(SOUND_DEATH)
    }

    fun setAlive()
    {
        life = FULL_LIFE
        layerMask = PLAYER_LAYER
        bodyType = BodyType.DYNAMIC
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

        // Graphics
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

        // Lighting
        p.intensity = intensity
        p.radius = radius
        p.size = size
        p.coneAngle = coneAngle
        p.spill = spill
        p.type = type
        p.shadowType = shadowType
        p.normalMapName = normalMapName
        p.normalMapIntensity = normalMapIntensity
        p.normalMapOrientation = normalMapOrientation

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
        private const val FULL_LIFE = 100f
    }
}