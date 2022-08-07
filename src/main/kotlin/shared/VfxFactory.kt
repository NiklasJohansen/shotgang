package shared

import entities.*
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.Extensions.toRadians
import no.njoh.pulseengine.core.shared.utils.MathUtil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object VfxFactory
{
    fun spawnShotgunEffects(engine: PulseEngine, x: Float, y: Float, rotationDegrees: Float)
    {
        // Muzzle flash
        val flash = Flash()
        flash.color = Color(1f, 0.85f, 0.3f)
        flash.lifeTimeMillis = 100f
        flash.coneAngle = 0f
        flash.intensity = 8f
        flash.radius = 400f + 300f * Random.nextFloat()
        flash.rotation = rotationDegrees
        flash.x = x
        flash.y = y
        flash.onStart(engine)
        engine.scene.addEntity(flash)

        // Sparks
        for (i in 0 until 15)
        {
            val spark = Spark()
            val velocity = 10f + Random.nextFloat() * 60
            val angle = -rotationDegrees + 10 * nextRandomGaussian()
            spark.turbulence = 5f
            spark.timeToLiveMillis = 50 + Random.nextLong(300)
            spark.init(x, y, angle.toRadians(), velocity)
            spark.onStart(engine)
            engine.scene.addEntity(spark)
        }

        // Smoke
        val rotationRad = rotationDegrees.toRadians()
        for (i in 0 until 10)
        {
            val smoke = Smoke()
            val velocity = 5f + 20f * Random.nextFloat()
            smoke.color.setFrom(0.6f, 0.6f, 0.6f, 0.2f)
            smoke.x = x
            smoke.y = y
            smoke.rotation = Random.nextFloat() * 360f
            smoke.startSize = 5f + 10f * Random.nextFloat()
            smoke.endSize = 80f + 80f * Random.nextFloat()
            smoke.lifeTimeMillis = 500 + (800 * Random.nextFloat()).toLong()
            smoke.drag = 0.15f + 0.05f * Random.nextFloat()
            smoke.init(engine)
            smoke.shape.xLast = x - velocity * cos(-rotationRad)
            smoke.shape.yLast = y - velocity * sin(-rotationRad)
            smoke.shape.rotLast = smoke.shape.rot - 0.03f * Random.nextFloat()
            engine.scene.addEntity(smoke)
        }

        // Eject empty shell
        val shell = Shell()
        shell.color = Color(0.7f, 0.7f, 0.7f)
        shell.x = x
        shell.y = y
        shell.z = 1f
        shell.width = 8f
        shell.height = 16f
        shell.rotation = rotationDegrees
        shell.init(engine)
        shell.shape.xLast = x - 35f * cos(-rotationRad + PIf / 2f)
        shell.shape.yLast = y - 35f * sin(-rotationRad + PIf / 2f)
        shell.shape.applyAngularAcceleration(nextRandomGaussian() * 0.5f)
        engine.scene.addEntity(shell)
    }

    fun spawnWallBloodEffect(engine: PulseEngine, x: Float, y: Float)
    {
        val wallBlood = Decal()
        wallBlood.color.setFrom(0.5f, 0.5f, 0.5f, 1f)
        wallBlood.textureName = TEXTURE_BLOOD_1
        wallBlood.x = x
        wallBlood.y = y
        wallBlood.width = 512f + 65 * nextRandomGaussian()
        wallBlood.height = 512f + 65 * nextRandomGaussian()
        wallBlood.rotation = 10f * nextRandomGaussian()
        wallBlood.onStart(engine)
        engine.scene.addEntity(wallBlood)
    }

    fun spawnGroundBloodEffect(engine: PulseEngine, x: Float, y: Float)
    {
        val groundBlood = Decoration()
        groundBlood.color.alpha = 0.2f
        groundBlood.textureName = TEXTURE_BLOOD_0
        groundBlood.x = x + 10f * nextRandomGaussian()
        groundBlood.y = y + 10f * nextRandomGaussian()
        groundBlood.z = 1f
        groundBlood.width = 200f + 10 * nextRandomGaussian()
        groundBlood.height = 200f + 10 * nextRandomGaussian()
        groundBlood.rotation = 20 * nextRandomGaussian()
        groundBlood.onStart(engine)
        engine.scene.addEntity(groundBlood)
    }

    fun spawnSpikeWallBloodEffect(engine: PulseEngine, x: Float, y: Float)
    {
        val blood = Decal()
        blood.color = Color(0.9f, 0.9f, 0.9f, 0.9f)
        blood.textureName = TEXTURE_BLOOD_0
        blood.x = x
        blood.y = y
        blood.width = 300f
        blood.height = 300f
        blood.rotation = Random.nextFloat() * 360f
        blood.onStart(engine)
        engine.scene.addEntity(blood)
    }

    fun spawnBloodSmokePuffEffect(engine: PulseEngine, x: Float, y: Float, xDir: Float, yDir: Float)
    {
        val blood = Smoke()
        blood.color = Color(1f, 0.4f + 0.3f * Random.nextFloat(), 0.4f + 0.3f * Random.nextFloat())
        blood.x = x
        blood.y = y
        blood.rotation = Random.nextFloat() * 360f
        blood.startSize = 10f + 15f * Random.nextFloat()
        blood.endSize = 40f + 40f * Random.nextFloat()
        blood.lifeTimeMillis = 500 + (800 * Random.nextFloat()).toLong()
        blood.init(engine)
        blood.shape.xLast = x - xDir * 3f
        blood.shape.yLast = y - yDir * 3f
        blood.shape.applyAngularAcceleration(0.03f * Random.nextFloat())
        engine.scene.addEntity(blood)
    }

    fun spawnBulletWallHitEffects(engine: PulseEngine, x: Float, y: Float, xDir: Float, yDir: Float)
    {
        // Smoke
        val smoke = Smoke()
        val velocity = 2f + 10f * Random.nextFloat()
        smoke.color.setFrom(0.5f, 0.5f, 0.5f, 0.8f)
        smoke.x = x
        smoke.y = y
        smoke.rotation = Random.nextFloat() * 360f
        smoke.startSize = 2f + 5f * Random.nextFloat()
        smoke.endSize = 30f + 20f * Random.nextFloat()
        smoke.lifeTimeMillis = 500 + (800 * Random.nextFloat()).toLong()
        smoke.init(engine)
        smoke.shape.applyAngularAcceleration(0.03f * Random.nextFloat())
        smoke.shape.xLast = x - velocity * xDir
        smoke.shape.yLast = y - velocity * yDir
        engine.scene.addEntity(smoke)

        // Flash
        val flash = Flash()
        flash.color = Color(1f, 0.7f, 0.3f)
        flash.lifeTimeMillis = 100f
        flash.coneAngle = 360f
        flash.intensity = 1f
        flash.radius = 300f
        flash.size = 100f
        flash.x = x + velocity * xDir
        flash.y = y + velocity * yDir
        flash.onStart(engine)
        engine.scene.addEntity(flash)

        // Sparks
        for (i in 0 until 2)
        {
            val spark = Spark()
            val sparkVelocity = 30f + Random.nextFloat() * 10f
            val sparkAngle = MathUtil.atan2(yDir, xDir) + 0.5f * PIf * nextRandomGaussian()
            spark.turbulence = 10f
            spark.drag = 0.3f
            spark.timeToLiveMillis = 100 + Random.nextLong(100)
            spark.init(x, y, sparkAngle, sparkVelocity)
            spark.onStart(engine)
            engine.scene.addEntity(spark)
        }

        // Hit decals
        val decal = Decal()
        decal.color = Color(1f, 1f, 1f, 0.8f +  0.2f * Random.nextFloat())
        decal.textureName = TEXTURE_BURN_0
        decal.x = x
        decal.y = y
        decal.width = 536 / 4f
        decal.height = 486 / 4f
        decal.rotation = Random.nextFloat() * 360f
        decal.onStart(engine)
        engine.scene.addEntity(decal)
    }
}