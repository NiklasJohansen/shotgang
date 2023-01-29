package shared

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.AssetManager
import no.njoh.pulseengine.core.asset.types.*
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.Extensions.loadFileNames
import kotlin.math.*

val PIf = PI.toFloat()
val random = java.util.Random()
val spriteSheetDimensionRegex = "_([0-9]{1,3})x([0-9]{1,3})\\.".toRegex() // Matches _1x2.

fun nextRandomGaussian() = random.nextGaussian().toFloat()

fun filterStickInput(xStick: Float, yStick: Float): Vec2
{
    val deadZone = 0.2f
    val length = sqrt(xStick * xStick + yStick * yStick)
    return if (length >= deadZone)
    {
        val angle = atan2(yStick, xStick)
        val scaledLength = (length - deadZone) / (1f - deadZone)
        val x = cos(angle) * scaledLength
        val y = -sin(angle) * scaledLength
        Vec2(x, y)
    }
    else Vec2(0f, 0f)
}

fun Surface2D.setDrawColor(color: Color, alpha: Float) =
    setDrawColor(color.red, color.green, color.blue, color.alpha * alpha)

fun PulseEngine.playSoundWithName(name: String, pitch: Float = 1f, volume: Float = 1f)
{
    val sound = asset.getOrNull<Sound>(name) ?: return
    val sourceId = audio.createSource(sound)
    audio.setPitch(sourceId, pitch)
    audio.setVolume(sourceId, volume)
    audio.play(sourceId)
}

inline fun <reified T: Asset> AssetManager.loadAll(directory: String)
{
    for (fileName in directory.loadFileNames())
    {
        val assetName = fileName.substringAfterLast("/").substringBeforeLast(".")
        when (T::class)
        {
            Font::class -> if (fileName.endsWith("ttf")) loadFont(fileName, assetName, fontSize = 80f)
            Sound::class -> if (fileName.endsWith("ogg")) loadSound(fileName, assetName)
            Texture::class -> if (!fileName.endsWith("png")) continue else
            {
                val dimensions = spriteSheetDimensionRegex.find(fileName) // example match: _8x2.
                if (dimensions == null)
                    loadTexture(fileName, assetName)
                else loadSpriteSheet(
                    fileName = fileName,
                    assetName = assetName.substringBeforeLast("_"),
                    horizontalCells = dimensions.groupValues[1].toInt(),
                    verticalCells = dimensions.groupValues[2].toInt()
                )
            }
        }
    }
}
